"""place_enrichment 기타인데 rag 메타에 음식 대분류가 있는지 샘플 조회."""
import sys
from pathlib import Path

from dotenv import load_dotenv

sys.path.insert(0, str(Path(__file__).resolve().parent))
from run_filtered_enrichment import connect_db  # noqa: E402


def main() -> None:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env")
    conn = connect_db()
    with conn.cursor() as cur:
        cur.execute(
            """
            select count(1)
            from place_enrichment pe
            join rag_documents rd
              on rd.source_type = pe.source_type
             and rd.source_id = pe.source_id
            where pe.recommended_category = '기타'
              and (
                coalesce(rd.metadata->>'cat1_name', '') ilike '%음식%'
                or coalesce(rd.metadata->>'content_type_name', '') ilike '%음식%'
                or coalesce(rd.metadata->>'cat1_code', '') = '39'
              )
            """
        )
        cnt = cur.fetchone()[0]

        cur.execute(
            """
            select pe.source_type, pe.source_id, pe.name,
                   pe.recommended_category,
                   rd.metadata->>'cat1_code' as c1,
                   rd.metadata->>'cat1_name' as n1,
                   rd.metadata->>'content_type_name' as ctype
            from place_enrichment pe
            join rag_documents rd
              on rd.source_type = pe.source_type
             and rd.source_id = pe.source_id
            where pe.recommended_category = '기타'
              and (
                coalesce(rd.metadata->>'cat1_name', '') ilike '%음식%'
                or coalesce(rd.metadata->>'content_type_name', '') ilike '%음식%'
                or coalesce(rd.metadata->>'cat1_code', '') = '39'
              )
            limit 8
            """
        )
        rows = cur.fetchall()

    conn.close()
    print("mismatch_count", cnt)
    for r in rows:
        print(r)


if __name__ == "__main__":
    main()
