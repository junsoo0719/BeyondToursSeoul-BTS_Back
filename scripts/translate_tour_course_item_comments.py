#!/usr/bin/env python3
"""
DB의 tour_course_items.ai_comment(한국어)를 읽어 Google Cloud Translation API v2로 번역하고
tour_course_item_translations(ENG, JPN, CHS, CHT)에 UPSERT 합니다.

사전 준비:
  1) DB.md의 tour_course_item_translations DDL 적용
  2) .env에 SUPABASE_DB_URL(jdbc), SUPABASE_DB_USERNAME, SUPABASE_DB_PASSWORD, GOOGLE_TRANSLATION_API_KEY

사용 예:
  cd BeyondToursSeoul-BTS_Back/scripts
  pip install -r requirements-translate-course-items.txt
  python translate_tour_course_item_comments.py --dry-run
  python translate_tour_course_item_comments.py
  python translate_tour_course_item_comments.py --overwrite
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import time
from pathlib import Path

import psycopg2
import requests
from dotenv import load_dotenv

# 프로젝트 루트(BeyondToursSeoul-BTS_Back)의 .env
_SCRIPT_DIR = Path(__file__).resolve().parent
_BACK_ROOT = _SCRIPT_DIR.parent
load_dotenv(_BACK_ROOT / ".env")

GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2"

LANG_TARGETS = [
    ("ENG", "en"),
    ("JPN", "ja"),
    ("CHS", "zh-CN"),
    ("CHT", "zh-TW"),
]


def parse_jdbc_postgresql(jdbc_url: str) -> tuple[str, int, str]:
    m = re.match(
        r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)",
        jdbc_url.strip(),
        re.IGNORECASE,
    )
    if not m:
        raise ValueError(
            "SUPABASE_DB_URL은 jdbc:postgresql://호스트:포트/DB 형식이어야 합니다."
        )
    return m.group(1), int(m.group(2)), m.group(3)


def connect_pg():
    jdbc = os.environ.get("SUPABASE_DB_URL", "").strip()
    user = os.environ.get("SUPABASE_DB_USERNAME", "").strip()
    password = os.environ.get("SUPABASE_DB_PASSWORD", "").strip()
    if not jdbc or not user:
        sys.exit("SUPABASE_DB_URL, SUPABASE_DB_USERNAME, SUPABASE_DB_PASSWORD를 설정하세요.")
    host, port, dbname = parse_jdbc_postgresql(jdbc)
    return psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=user,
        password=password,
        sslmode="require",
        connect_timeout=30,
    )


def fetch_all_comment_items(conn) -> list[tuple[int, str]]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT i.id, i.ai_comment
            FROM tour_course_items i
            WHERE i.ai_comment IS NOT NULL AND btrim(i.ai_comment) <> ''
            ORDER BY i.id
            """
        )
        return [(int(r[0]), str(r[1])) for r in cur.fetchall()]


def ids_missing_language(conn, ids: tuple[int, ...], db_lang: str) -> set[int]:
    if not ids:
        return set()
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT i.id FROM tour_course_items i
            WHERE i.id IN %s
              AND NOT EXISTS (
                SELECT 1 FROM tour_course_item_translations t
                WHERE t.course_item_id = i.id AND t.language = %s
              )
            """,
            (ids, db_lang),
        )
        return {int(r[0]) for r in cur.fetchall()}


def google_translate_batch(
    api_key: str, texts: list[str], target: str, source: str = "ko"
) -> list[str]:
    if not texts:
        return []
    params = {"key": api_key}
    body = {"q": texts, "source": source, "target": target, "format": "text"}
    r = requests.post(GOOGLE_TRANSLATE_URL, params=params, json=body, timeout=120)
    r.raise_for_status()
    data = r.json()
    trans = data.get("data", {}).get("translations", [])
    if len(trans) != len(texts):
        raise RuntimeError(
            f"번역 결과 개수 불일치: 요청 {len(texts)} / 응답 {len(trans)}"
        )
    return [t.get("translatedText", "") for t in trans]


def upsert_translations(
    conn,
    rows: list[tuple[int, str]],
    lang_code: str,
    dry_run: bool,
) -> int:
    if dry_run or not rows:
        return 0
    sql = """
        INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
        VALUES (%s, %s, %s)
        ON CONFLICT (course_item_id, language)
        DO UPDATE SET ai_comment = EXCLUDED.ai_comment
    """
    with conn.cursor() as cur:
        cur.executemany(sql, [(iid, lang_code, txt) for iid, txt in rows])
    conn.commit()
    return len(rows)


def chunked(seq: list, size: int):
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


def main() -> None:
    ap = argparse.ArgumentParser(description="코스 아이템 ai_comment 일괄 번역")
    ap.add_argument(
        "--dry-run",
        action="store_true",
        help="DB 연결·대상 건수만 확인하고 API/INSERT 안 함",
    )
    ap.add_argument(
        "--overwrite",
        action="store_true",
        help="이미 번역 행이 있어도 덮어씀(기본은 없는 언어만 채움)",
    )
    ap.add_argument(
        "--batch-size",
        type=int,
        default=40,
        help="Google API 한 번에 보낼 문장 수(기본 40)",
    )
    ap.add_argument(
        "--sleep",
        type=float,
        default=0.35,
        help="배치 사이 초 단위 대기(쿼터 완화)",
    )
    args = ap.parse_args()

    conn = connect_pg()
    try:
        base_rows = fetch_all_comment_items(conn)

        if not base_rows:
            print("번역할 tour_course_items 행이 없습니다.")
            return

        print(f"원문(한국어) 스텝: {len(base_rows)}건")

        if args.dry_run:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT COUNT(*) FROM tour_course_item_translations"
                )
                (cnt,) = cur.fetchone()
            print(f"기존 번역 행: {int(cnt)}건 (dry-run 종료)")
            return

        api_key = os.environ.get("GOOGLE_TRANSLATION_API_KEY", "").strip()
        if not api_key:
            sys.exit("GOOGLE_TRANSLATION_API_KEY가 없습니다. .env를 확인하세요.")

        ids_tuple = tuple(r[0] for r in base_rows)

        for db_lang, google_target in LANG_TARGETS:
            if not args.overwrite:
                need_ids = ids_missing_language(conn, ids_tuple, db_lang)
                pairs = [(i, c) for i, c in base_rows if i in need_ids]
            else:
                pairs = list(base_rows)

            if not pairs:
                print(f"[{db_lang}] 스킵 — 모두 존재하거나 대상 없음")
                continue

            out_pairs: list[tuple[int, str]] = []
            batch_n = 0
            for chunk in chunked(pairs, args.batch_size):
                batch_n += 1
                chunk_ids = [p[0] for p in chunk]
                chunk_texts = [p[1] for p in chunk]
                try:
                    translated = google_translate_batch(
                        api_key, chunk_texts, google_target
                    )
                except requests.HTTPError as e:
                    print(f"[{db_lang}] HTTP 오류 배치 {batch_n}: {e}", file=sys.stderr)
                    if e.response is not None:
                        print(e.response.text[:2000], file=sys.stderr)
                    sys.exit(1)
                for iid, txt in zip(chunk_ids, translated, strict=True):
                    out_pairs.append((iid, txt))
                print(f"[{db_lang}] 배치 {batch_n} ({len(chunk)}건) 완료")
                time.sleep(args.sleep)

            n = upsert_translations(conn, out_pairs, db_lang, dry_run=False)
            print(f"[{db_lang}] UPSERT {n}건")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
