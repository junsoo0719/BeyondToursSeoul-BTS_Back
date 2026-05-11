import argparse
import os
from pathlib import Path
from urllib.parse import urlparse

import psycopg
from dotenv import load_dotenv
from psycopg.rows import dict_row

from enrich_supabase_data import SourceRecord, enrich_record, upsert_results, write_json


def connect_db() -> psycopg.Connection:
    dsn = os.getenv("SUPABASE_DB_URL") or os.getenv("DATABASE_URL")
    if dsn:
        if dsn.startswith("jdbc:"):
            dsn = dsn[len("jdbc:") :]
        parsed = urlparse(dsn if "://" in dsn else f"postgresql://{dsn}")
        if parsed.hostname:
            db_user = parsed.username or os.getenv("SUPABASE_DB_USERNAME") or os.getenv("DB_USER")
            db_password = parsed.password or os.getenv("SUPABASE_DB_PASSWORD") or os.getenv("DB_PASSWORD")
            db_name = parsed.path.lstrip("/") or os.getenv("DB_NAME") or "postgres"
            db_port = parsed.port or int(os.getenv("DB_PORT", "5432"))
            if not all([db_user, db_password]):
                raise RuntimeError("DB 계정정보(user/password)가 없습니다.")
            return psycopg.connect(
                host=parsed.hostname,
                port=db_port,
                dbname=db_name,
                user=db_user,
                password=db_password,
                sslmode=os.getenv("DB_SSLMODE", "require"),
            )

    if dsn and dsn.startswith("aws-"):
        parsed = urlparse(f"postgresql://{dsn}")
        return psycopg.connect(
            host=parsed.hostname,
            port=parsed.port or 5432,
            dbname=parsed.path.lstrip("/") or "postgres",
            user=os.getenv("SUPABASE_DB_USERNAME") or os.getenv("DB_USER"),
            password=os.getenv("SUPABASE_DB_PASSWORD") or os.getenv("DB_PASSWORD"),
            sslmode=os.getenv("DB_SSLMODE", "require"),
        )

    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME")
    db_user = os.getenv("SUPABASE_DB_USERNAME") or os.getenv("DB_USER")
    db_password = os.getenv("SUPABASE_DB_PASSWORD") or os.getenv("DB_PASSWORD")
    if not all([db_host, db_name, db_user, db_password]):
        raise RuntimeError("DB 연결 환경변수가 부족합니다.")
    return psycopg.connect(
        host=db_host,
        port=db_port,
        dbname=db_name,
        user=db_user,
        password=db_password,
        sslmode=os.getenv("DB_SSLMODE", "require"),
    )


def load_filtered_events(
    conn: psycopg.Connection,
    cutoff: str,
    limit: int,
    *,
    skip_existing: bool = False,
) -> list[SourceRecord]:
    skip_frag = ""
    if skip_existing:
        skip_frag = """
              and not exists (
                select 1 from place_enrichment pe
                where pe.source_type = 'tour_event'
                  and pe.source_id = coalesce(t.content_id::text, t.id::text)
              )
        """
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            f"""
            select
                t.id,
                t.content_id,
                t.title,
                t.address,
                t.overview,
                t.homepage,
                rd.metadata as rag_metadata
            from tour_api_event_translation t
            join tour_api_event e on e.content_id = t.content_id
            left join lateral (
                select metadata
                from rag_documents
                where source_type = 'event'
                  and source_id = coalesce(t.content_id::text, t.id::text)
                limit 1
            ) rd on true
            where t.language = 'KOR'
              and coalesce(e.event_start_date, '') <> ''
              and to_date(e.event_start_date, 'YYYYMMDD') >= to_date(%s, 'YYYY-MM-DD')
              {skip_frag}
            order by e.event_start_date asc, t.id asc
            limit %s
            """,
            (cutoff, limit),
        )
        rows = cur.fetchall()

    out: list[SourceRecord] = []
    for r in rows:
        meta = r.get("rag_metadata") or {}
        if not isinstance(meta, dict):
            meta = {}
        hint_parts = [
            meta.get("cat1_name") or "",
            meta.get("cat2_name") or "",
            meta.get("cat3_name") or "",
            meta.get("content_type_name") or "",
        ]
        hint = " ".join(p for p in hint_parts if p)
        out.append(
            SourceRecord(
                source_type="tour_event",
                source_id=str(r["content_id"] or r["id"]),
                name=r["title"] or "",
                address=r["address"] or "",
                content=r["overview"] or "",
                homepage=r["homepage"],
                source_category_hint=hint,
            )
        )
    return out


def load_filtered_attractions(
    conn: psycopg.Connection,
    limit: int,
    *,
    skip_existing: bool = False,
) -> list[SourceRecord]:
    event_patterns = ["%행사%", "%전시%", "%축제%", "%이벤트%", "%공연%"]
    locker_patterns = ["%보관%", "%물품%", "%locker%"]
    skip_frag = ""
    if skip_existing:
        skip_frag = """
              and not exists (
                select 1 from place_enrichment pe
                where pe.source_type = 'attraction'
                  and pe.source_id = a.id::text
              )
        """
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            f"""
            with excluded_ids as (
                select distinct source_id::bigint as attraction_id
                from rag_documents
                where source_type = 'attraction'
                  and (
                    coalesce(title, '') ilike any(%s)
                    or coalesce(content, '') ilike any(%s)
                    or coalesce(metadata->>'cat1_name', '') ilike any(%s)
                    or coalesce(metadata->>'content_type_name', '') ilike any(%s)
                  )
            )
            select
                a.id,
                a.name,
                a.address,
                a.overview,
                a.category,
                rd.metadata as rag_metadata
            from attraction a
            left join lateral (
                select metadata
                from rag_documents
                where source_type = 'attraction'
                  and source_id = a.id::text
                limit 1
            ) rd on true
            left join excluded_ids ex on ex.attraction_id = a.id
            where ex.attraction_id is null
              and coalesce(a.name, '') not ilike any(%s)
              and coalesce(a.category, '') not ilike any(%s)
              and coalesce(a.address, '') not ilike any(%s)
              {skip_frag}
            order by a.id asc
            limit %s
            """,
            (
                event_patterns,
                event_patterns,
                event_patterns,
                event_patterns,
                locker_patterns,
                locker_patterns,
                locker_patterns,
                limit,
            ),
        )
        rows = cur.fetchall()

    out_a: list[SourceRecord] = []
    for r in rows:
        meta = r.get("rag_metadata") or {}
        if not isinstance(meta, dict):
            meta = {}
        hint_parts = [
            r.get("category") or "",
            meta.get("cat1_name") or "",
            meta.get("cat2_name") or "",
            meta.get("cat3_name") or "",
            meta.get("content_type_name") or "",
        ]
        hint = " ".join(p for p in hint_parts if p)
        out_a.append(
            SourceRecord(
                source_type="attraction",
                source_id=str(r["id"]),
                name=r["name"] or "",
                address=r["address"] or "",
                content=f"{r['overview'] or ''} {r['category'] or ''}",
                source_category_hint=hint,
            )
        )
    return out_a


def main() -> None:
    script_dir = Path(__file__).resolve().parent
    load_dotenv(script_dir / ".env", override=False)
    load_dotenv(script_dir.parent.parent / ".env", override=False)

    parser = argparse.ArgumentParser(description="배제 조건 적용 enrichment 실행기")
    parser.add_argument("--target-total", type=int, default=3000)
    parser.add_argument("--event-cutoff", default="2026-05-15")
    parser.add_argument("--write-json", default="./out/enrichment_filtered_3000.json")
    parser.add_argument("--upsert-table", default="place_enrichment")
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="place_enrichment 에 이미 있는 source_type+source_id 는 제외",
    )
    args = parser.parse_args()

    conn = connect_db()
    try:
        event_records = load_filtered_events(
            conn,
            args.event_cutoff,
            args.target_total,
            skip_existing=args.skip_existing,
        )
        attraction_needed = max(0, args.target_total - len(event_records))
        attraction_records = load_filtered_attractions(
            conn,
            attraction_needed,
            skip_existing=args.skip_existing,
        )

        records = event_records + attraction_records
        enriched = [enrich_record(r) for r in records if r.name]

        write_json(args.write_json, enriched)
        upsert_results(conn, args.upsert_table, enriched)

        print(f"[DONE] target={args.target_total}, input={len(records)}, enriched={len(enriched)}")
        print(f"[SPLIT] events={len(event_records)}, attractions={len(attraction_records)}")
        print(f"[JSON] {args.write_json}")
        print(f"[UPSERT] table={args.upsert_table}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
