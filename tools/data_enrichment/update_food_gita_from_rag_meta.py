"""
place_enrichment 에서 recommended_category 가 '기타' 인데,
rag_documents.metadata 가 음식 대분류인 행만 '음식점' 으로 UPDATE.

조건은 run_rag_rebuild_enrichment.force_category_from_hint 의 음식 매핑과 맞춤:
  - cat1_code / content_type_code 가 39 또는 FD (대소문자 무시)
  - cat1_name / content_type_name 이 정확히 '음식' 또는 '음식점'

예:
  python update_food_gita_from_rag_meta.py --dry-run
  python update_food_gita_from_rag_meta.py
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from dotenv import load_dotenv

sys.path.insert(0, str(Path(__file__).resolve().parent))
from run_filtered_enrichment import connect_db  # noqa: E402


def _food_meta_exists_sql() -> str:
    return """
        exists (
            select 1
            from rag_documents rd
            where rd.source_type = pe.source_type
              and rd.source_id = pe.source_id
              and (
                upper(btrim(coalesce(rd.metadata->>'cat1_code', ''))) in ('39', 'FD')
                or upper(btrim(coalesce(rd.metadata->>'content_type_code', ''))) in ('39', 'FD')
                or btrim(coalesce(rd.metadata->>'cat1_name', '')) in ('음식', '음식점')
                or btrim(coalesce(rd.metadata->>'content_type_name', '')) in ('음식', '음식점')
              )
        )
    """


def main() -> None:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env")
    parser = argparse.ArgumentParser(
        description="RAG 메타가 음식 대분류인데 place_enrichment 가 기타인 행만 음식점으로 수정",
    )
    parser.add_argument("--table", default="place_enrichment", help="갱신 대상 테이블")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="UPDATE 없이 대상 건수만 조회",
    )
    args = parser.parse_args()
    if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", args.table):
        raise SystemExit("테이블 이름이 안전하지 않습니다.")

    conn = connect_db()
    try:
        cond = _food_meta_exists_sql()
        with conn.cursor() as cur:
            cur.execute(
                f"""
                select count(1)
                from {args.table} pe
                where pe.recommended_category = '기타'
                  and {cond}
                """
            )
            n_match = cur.fetchone()[0]

        if args.dry_run:
            print(f"[DRY-RUN] 대상 건수: {n_match} (테이블={args.table})")
            return

        with conn.cursor() as cur:
            cur.execute(
                f"""
                update {args.table} pe
                set recommended_category = '음식점',
                    updated_at = now()
                where pe.recommended_category = '기타'
                  and {cond}
                """
            )
            updated = cur.rowcount
        conn.commit()
        print(f"[DONE] updated={updated} (선택 건수와 동일해야 함: {n_match})")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
