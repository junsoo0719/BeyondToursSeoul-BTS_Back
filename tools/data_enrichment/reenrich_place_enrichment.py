"""
place_enrichment 행을 다시 검색·크롤링해 recommended_category 등을 재계산 후 upsert.

잘못 SQL로 기타만 박아 넣은 경우, 이 스크립트로 원래 파이프라인을 다시 태우면
맛집 금지 시 키워드 2순위 카테고리가 반영된다.
"""
from __future__ import annotations

import argparse
import time
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from psycopg.rows import dict_row

from enrich_supabase_data import SourceRecord, enrich_record, upsert_results
from run_filtered_enrichment import connect_db


def load_source_record_attraction(conn, source_id: str) -> SourceRecord | None:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
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
            where a.id = %s
            """,
            (int(source_id),),
        )
        r = cur.fetchone()
    if not r:
        return None
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
    return SourceRecord(
        source_type="attraction",
        source_id=str(r["id"]),
        name=r["name"] or "",
        address=r["address"] or "",
        content=f"{r['overview'] or ''} {r['category'] or ''}",
        source_category_hint=hint,
    )


def load_source_record_event(conn, source_id: str) -> SourceRecord | None:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            select
                t.content_id,
                t.id,
                t.title,
                t.address,
                t.overview,
                t.homepage,
                rd.metadata as rag_metadata
            from tour_api_event_translation t
            left join lateral (
                select metadata
                from rag_documents
                where source_type = 'event'
                  and source_id = coalesce(t.content_id::text, t.id::text)
                limit 1
            ) rd on true
            where t.language = 'KOR'
              and coalesce(t.content_id::text, t.id::text) = %s
            limit 1
            """,
            (source_id,),
        )
        r = cur.fetchone()
    if not r:
        return None
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
    return SourceRecord(
        source_type="tour_event",
        source_id=str(r["content_id"] or r["id"]),
        name=r["title"] or "",
        address=r["address"] or "",
        content=r["overview"] or "",
        homepage=r["homepage"],
        source_category_hint=hint,
    )


FOOD_HINT_ATTR = """(
  coalesce(a.category, '') ilike %s
  or coalesce(rd.metadata->>'cat1_name', '') ilike %s
  or coalesce(rd.metadata->>'cat2_name', '') ilike %s
  or coalesce(rd.metadata->>'cat3_name', '') ilike %s
  or coalesce(rd.metadata->>'content_type_name', '') ilike %s
)"""


def _optional_category_filter(only_category: str | None, column: str = "recommended_category") -> tuple[str, list[Any]]:
    """NULL 바인딩 타입 모호함 회피용."""
    if only_category is None:
        return "", []
    return f"and {column} = %s", [only_category]


def fetch_targets(conn, *, limit: int, only_category: str | None, source_types: list[str]) -> list[tuple[str, str]]:
    frag, extra = _optional_category_filter(only_category)
    with conn.cursor() as cur:
        cur.execute(
            f"""
            select source_type, source_id
            from place_enrichment
            where source_type = any(%s)
            {frag}
            order by updated_at desc
            limit %s
            """,
            (source_types, *extra, limit),
        )
        return [(row[0], row[1]) for row in cur.fetchall()]


def fetch_targets_restaurant_only(
    conn,
    *,
    limit: int,
    only_category: str | None,
    source_types: list[str],
) -> list[tuple[str, str]]:
    """원본 분류에 '음식점'이 있는 place_enrichment 행만 (맛집 재평가용)."""
    food = "%음식점%"
    out: list[tuple[str, str]] = []
    remaining = limit
    cat_frag, cat_extra = _optional_category_filter(only_category, "pe.recommended_category")

    with conn.cursor() as cur:
        if "attraction" in source_types and remaining > 0:
            cur.execute(
                f"""
                select pe.source_type, pe.source_id
                from place_enrichment pe
                join attraction a on pe.source_type = 'attraction' and pe.source_id = a.id::text
                left join lateral (
                    select metadata
                    from rag_documents
                    where source_type = 'attraction' and source_id = a.id::text
                    limit 1
                ) rd on true
                where 1=1
                  {cat_frag}
                  and {FOOD_HINT_ATTR}
                order by pe.updated_at desc
                limit %s
                """,
                (
                    *cat_extra,
                    food,
                    food,
                    food,
                    food,
                    food,
                    remaining,
                ),
            )
            out.extend([(row[0], row[1]) for row in cur.fetchall()])
            remaining = limit - len(out)

        if "tour_event" in source_types and remaining > 0:
            cur.execute(
                f"""
                select pe.source_type, pe.source_id
                from place_enrichment pe
                join tour_api_event_translation t
                  on pe.source_type = 'tour_event'
                 and pe.source_id = coalesce(t.content_id::text, t.id::text)
                left join lateral (
                    select metadata
                    from rag_documents
                    where source_type = 'event'
                      and source_id = coalesce(t.content_id::text, t.id::text)
                    limit 1
                ) rd on true
                where t.language = 'KOR'
                  {cat_frag}
                  and (
                    coalesce(rd.metadata->>'cat1_name', '') ilike %s
                    or coalesce(rd.metadata->>'cat2_name', '') ilike %s
                    or coalesce(rd.metadata->>'cat3_name', '') ilike %s
                    or coalesce(rd.metadata->>'content_type_name', '') ilike %s
                  )
                order by pe.updated_at desc
                limit %s
                """,
                (
                    *cat_extra,
                    food,
                    food,
                    food,
                    food,
                    remaining,
                ),
            )
            out.extend([(row[0], row[1]) for row in cur.fetchall()])

    return out[:limit]


def main() -> None:
    base = Path(__file__).resolve().parent.parent.parent
    load_dotenv(base / ".env")

    parser = argparse.ArgumentParser(description="place_enrichment 행 재-enrich 후 upsert")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument(
        "--only-category",
        default="기타",
        help="recommended_category가 이 값인 행만 (전체는 --no-category-filter)",
    )
    parser.add_argument(
        "--no-category-filter",
        action="store_true",
        help="카테고리 조건 없이 limit 만큼 최근 갱신 순으로 재처리",
    )
    parser.add_argument(
        "--only-restaurant-source",
        action="store_true",
        help="원본(attraction/category·rag 또는 event/rag)에 '음식점'이 있는 행만 재처리",
    )
    parser.add_argument(
        "--source-types",
        default="attraction,tour_event",
        help="쉼표 구분: attraction, tour_event",
    )
    parser.add_argument("--upsert-table", default="place_enrichment")
    parser.add_argument("--sleep-between", type=float, default=0.0, help="레코드 간 초 대기")
    args = parser.parse_args()

    stypes = [s.strip() for s in args.source_types.split(",") if s.strip()]
    only_cat = None if args.no_category_filter else (args.only_category or None)

    conn = connect_db()
    if args.only_restaurant_source:
        print("[mode] 원본 분류에 '음식점' 포함된 행만 재-enrich", flush=True)
        targets = fetch_targets_restaurant_only(
            conn,
            limit=args.limit,
            only_category=only_cat,
            source_types=stypes,
        )
    else:
        targets = fetch_targets(conn, limit=args.limit, only_category=only_cat, source_types=stypes)
    if not targets:
        print("[SKIP] 대상 행 없음")
        conn.close()
        return

    enriched: list = []
    skipped = 0
    for i, (st, sid) in enumerate(targets, 1):
        if st == "attraction":
            rec = load_source_record_attraction(conn, sid)
        elif st == "tour_event":
            rec = load_source_record_event(conn, sid)
        else:
            skipped += 1
            continue
        if not rec or not rec.name:
            skipped += 1
            continue
        enriched.append(enrich_record(rec))
        if args.sleep_between > 0:
            time.sleep(args.sleep_between)
        if i % 20 == 0:
            print(f"[progress] {i}/{len(targets)}", flush=True)

    if enriched:
        upsert_results(conn, args.upsert_table, enriched)
    conn.close()

    print(f"[DONE] targets={len(targets)}, enriched={len(enriched)}, skipped={skipped}")
    print(f"[UPSERT] table={args.upsert_table}")


if __name__ == "__main__":
    main()
