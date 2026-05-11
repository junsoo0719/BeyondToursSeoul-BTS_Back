"""사용 중단: DB를 일괄 '기타'로 바꾸면 재분류 정보가 사라집니다.

대신 맛집 게이트만 적용된 파이프라인으로 다시 돌리세요:

  python tools/data_enrichment/reenrich_place_enrichment.py --limit 700 --only-category 기타

전량 재처리:

  python tools/data_enrichment/run_filtered_enrichment.py --target-total 3000 ...
"""
import sys


def main() -> None:
    print(
        "이 스크립트는 더 이상 DB를 수정하지 않습니다. "
        "reenrich_place_enrichment.py 를 사용하세요.",
        file=sys.stderr,
    )
    sys.exit(1)


if __name__ == "__main__":
    main()
