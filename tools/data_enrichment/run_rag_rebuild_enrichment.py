import argparse
import re
from pathlib import Path

from dotenv import load_dotenv
from psycopg.rows import dict_row

from enrich_supabase_data import SourceRecord, enrich_record, upsert_results, write_json
from run_filtered_enrichment import connect_db


def force_category_from_hint(hint: str, meta: dict | None = None) -> str | None:
    """RAG metadata 기준 쇼핑/음식점 강제 매핑 (TourAPI 코드·한글 대분류)."""
    meta = meta or {}
    t = (hint or "").strip()
    tl = t.lower()

    c1 = str(meta.get("cat1_code") or "").strip().upper()
    ct = str(meta.get("content_type_code") or "").strip().upper()
    n1 = (meta.get("cat1_name") or "").strip()
    ctype = (meta.get("content_type_name") or "").strip()

    # 숫자 코드: 38=쇼핑, 39=음식점 (문서 기준)
    if c1 == "38" or ct == "38" or "쇼핑" in t or "쇼핑" in n1 or "쇼핑" in ctype:
        return "쇼핑"
    # 일부 적재 데이터는 음식 대분류가 cat1_code=FD 로 옴
    if c1 in {"39", "FD"} or ct in {"39", "FD"}:
        return "음식점"

    if "쇼핑" in t:
        return "쇼핑"
    if "음식점" in t or "restaurant" in tl:
        return "음식점"
    # 예: "[콘텐츠 유형] 음식", 대분류 텍스트만 "음식"
    if "콘텐츠 유형" in t and "음식" in t:
        return "음식점"
    if n1 == "음식" or ctype == "음식":
        return "음식점"
    # TourAPI 코드 문자열이 hint에만 붙은 경우
    if "39" in t and ("cat1_code" in tl or "content_type_code" in tl):
        return "음식점"
    if "fd" in tl and ("cat1_code" in tl or "content_type_code" in tl):
        return "음식점"
    return None


def load_records_from_rag(conn, end_date_cutoff: str) -> list[SourceRecord]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            select
                source_type,
                source_id,
                coalesce(title, '') as title,
                coalesce(content, '') as content,
                metadata
            from rag_documents
            where source_type <> 'locker'
              and (
                source_type <> 'event'
                or coalesce(metadata->>'event_end_date', '') = ''
                or to_date(metadata->>'event_end_date', 'YYYYMMDD') > to_date(%s, 'YYYY-MM-DD')
              )
            order by id asc
            """,
            (end_date_cutoff,),
        )
        rows = cur.fetchall()

    out: list[SourceRecord] = []
    for r in rows:
        meta = r.get("metadata") or {}
        if not isinstance(meta, dict):
            meta = {}

        hint_parts = [
            f"cat1_code={meta.get('cat1_code') or ''}",
            f"content_type_code={meta.get('content_type_code') or ''}",
            meta.get("cat1_name") or "",
            meta.get("cat2_name") or "",
            meta.get("cat3_name") or "",
            meta.get("content_type_name") or "",
        ]
        hint = " ".join(p for p in hint_parts if p)

        address = (
            meta.get("address")
            or meta.get("addr1")
            or meta.get("road_address")
            or ""
        )

        homepage = meta.get("homepage") or None

        out.append(
            SourceRecord(
                source_type=str(r["source_type"]),
                source_id=str(r["source_id"]),
                name=r["title"] or "",
                address=address,
                content=r["content"] or "",
                homepage=homepage,
                source_category_hint=hint,
                rag_meta=meta,
            )
        )
    return out


def main() -> None:
    base = Path(__file__).resolve().parents[2]
    load_dotenv(base / ".env")

    parser = argparse.ArgumentParser(description="rag_documents 기반 place_enrichment 전체 재생성")
    parser.add_argument("--event-end-cutoff", default="2026-05-15")
    parser.add_argument("--upsert-table", default="place_enrichment")
    parser.add_argument("--write-json", default="./out/enrichment_rag_rebuild.json")
    parser.add_argument("--clear-existing", action="store_true", default=True)
    args = parser.parse_args()

    conn = connect_db()
    try:
        records = load_records_from_rag(conn, args.event_end_cutoff)
        enriched = []
        for rec in records:
            if not rec.name:
                continue
            row = enrich_record(rec)
            meta = rec.rag_meta if isinstance(rec.rag_meta, dict) else {}
            forced = force_category_from_hint(rec.source_category_hint, meta=meta)
            if forced:
                row["recommended_category"] = forced
            enriched.append(row)

        if args.clear_existing:
            if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", args.upsert_table):
                raise ValueError("삭제 대상 테이블 이름이 안전하지 않습니다.")
            with conn.cursor() as cur:
                cur.execute(f"delete from {args.upsert_table}")
            conn.commit()

        write_json(args.write_json, enriched)
        upsert_results(conn, args.upsert_table, enriched)

        print(f"[DONE] input={len(records)}, enriched={len(enriched)}")
        print(f"[JSON] {args.write_json}")
        print(f"[UPSERT] table={args.upsert_table}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
