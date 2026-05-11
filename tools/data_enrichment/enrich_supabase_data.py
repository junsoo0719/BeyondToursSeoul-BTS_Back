import argparse
import json
import os
import re
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests
from bs4 import BeautifulSoup
from dotenv import load_dotenv
from duckduckgo_search import DDGS
import psycopg
from psycopg.rows import dict_row


SEOUL_DISTRICTS = [
    "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구",
    "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구",
    "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구",
]

CATEGORY_RULES = {
    "카페": ["카페", "커피", "디저트", "브런치", "cafe", "coffee", "dessert"],
    "맛집": ["맛집", "음식", "레스토랑", "식당", "food", "restaurant", "미식"],
    "쇼핑": ["쇼핑", "market", "mall", "백화점", "상점"],
    "역사/문화": ["궁", "유적", "전통", "사찰", "문화재", "history", "heritage", "박물관"],
    "자연": ["공원", "산책", "한강", "숲", "trail", "park", "자연", "생태"],
    "예술/전시": ["전시", "미술", "갤러리", "museum", "exhibition", "아트", "아트홀"],
    "K-팝/아이돌": ["k-pop", "kpop", "아이돌", "콘서트", "팬미팅", "한류", "댄스", "공연"],
    "액티비티": ["체험", "액티비티", "클라이밍", "놀이", "sports", "게임", "테마파크"],
    "힐링": ["힐링", "휴식", "명상", "스파", "온천", "relax", "치유"],
    "야경/바": ["야경", "루프탑", "바", "칵테일", "night view", "pub", "bar", "클럽"],
    "포토스팟": ["포토스팟", "사진", "뷰포인트", "인생샷", "photo spot", "전망대", "스냅"],
    "로컬 탐방": ["로컬", "동네", "시장", "골목", "현지인", "탐방", "neighborhood"],
}

COMPANION_RULES = {
    "solo": ["혼자", "1인", "single", "개인 여행", "조용한"],
    "couple": ["커플", "데이트", "romantic", "연인"],
    "friends": ["친구", "단체", "액티비티", "체험"],
    "family": ["가족", "아이", "유아", "키즈", "어린이"],
}

PUBLIC_TRANSPORT_HINTS = [
    "지하철", "역", "도보", "버스", "환승", "subway", "metro", "station", "walkable",
]
CAR_HINTS = [
    "주차", "parking", "자차", "drive", "차량", "고속도로",
]


@dataclass
class SourceRecord:
    source_type: str
    source_id: str
    name: str
    address: str
    content: str
    homepage: str | None = None
    # 관광청/RAG 분류 등 원본 카테고리 힌트 (맛집 허용 여부 판단용)
    source_category_hint: str = ""
    # RAG 등에서 온 원본 metadata (강제 카테고리 매핑용, 선택)
    rag_meta: dict | None = None


LOW_QUALITY_DOMAINS = {
    "seoul.go.kr",
    "namu.wiki",
}

HIGH_QUALITY_HINTS = [
    "visitseoul",
    "kto.visitkorea",
    "korean.visitkorea",
    "tripadvisor",
    "mangoplate",
    "naver",
]


def normalize_text(s: str) -> str:
    return re.sub(r"\s+", " ", (s or "")).strip().lower()


def count_keyword_hits(text: str, keywords: list[str]) -> int:
    t = normalize_text(text)
    return sum(1 for kw in keywords if kw.lower() in t)


def extract_main_text(html: str) -> str:
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(["script", "style", "noscript"]):
        tag.decompose()
    text = soup.get_text(" ", strip=True)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:2500]


def fetch_url_text(url: str, timeout_sec: int = 5) -> str:
    try:
        headers = {"User-Agent": "Mozilla/5.0 (compatible; BTS-Enrichment/1.0)"}
        resp = requests.get(url, headers=headers, timeout=timeout_sec)
        if resp.status_code != 200:
            return ""
        # Some Korean pages expose wrong charset headers; force apparent encoding first.
        if resp.apparent_encoding:
            resp.encoding = resp.apparent_encoding
        return extract_main_text(resp.text)
    except Exception:
        return ""


def ddg_search_snippets(query: str, max_results: int = 3) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    try:
        with DDGS() as ddgs:
            results = ddgs.text(query, max_results=max_results)
            for r in results:
                out.append({
                    "title": r.get("title", ""),
                    "href": r.get("href", ""),
                    "body": r.get("body", ""),
                })
    except Exception:
        return []
    return out


def is_relevant_snippet(record: SourceRecord, snippet: dict[str, Any]) -> bool:
    href = (snippet.get("href") or "").lower()
    title = normalize_text(snippet.get("title", ""))
    body = normalize_text(snippet.get("body", ""))
    haystack = f"{title} {body} {href}"

    if any(domain in href for domain in LOW_QUALITY_DOMAINS):
        return False
    if any(hint in href for hint in HIGH_QUALITY_HINTS):
        return True

    name_tokens = [t for t in normalize_text(record.name).split(" ") if len(t) >= 2]
    address_tokens = [t for t in normalize_text(record.address).split(" ") if len(t) >= 2]

    token_hits = sum(1 for t in name_tokens[:4] if t in haystack)
    token_hits += sum(1 for t in address_tokens[:3] if t in haystack)
    # Allow slightly broader recall while still requiring some relevance.
    return token_hits >= 1 or ("서울" in (record.address or "") and "seoul" in href)


def allows_matjip_from_source_category(hint: str) -> bool:
    """원본 분류가 음식점 계열일 때만 맛집 카테고리 허용."""
    if not (hint or "").strip():
        return False
    t = normalize_text(hint)
    if "음식점" in (hint or "") or "음식점" in t:
        return True
    if "음식점>" in (hint or "") or "음식점>" in t:
        return True
    if "restaurant" in t:
        return True
    return False


def infer_category(text_blob: str, fallback: str = "기타", source_category_hint: str = "") -> str:
    scored = []
    for cat, kws in CATEGORY_RULES.items():
        scored.append((cat, count_keyword_hits(text_blob, kws)))
    scored.sort(key=lambda x: x[1], reverse=True)
    if not scored or scored[0][1] == 0:
        return fallback
    winner, top_score = scored[0][0], scored[0][1]
    if winner == "맛집" and not allows_matjip_from_source_category(source_category_hint):
        rest = [(c, s) for c, s in scored if c != "맛집" and s > 0]
        if rest:
            return rest[0][0]
        return fallback
    return winner


def infer_companions(text_blob: str) -> list[str]:
    picks: list[str] = []
    for tag, kws in COMPANION_RULES.items():
        if count_keyword_hits(text_blob, kws) > 0:
            picks.append(tag)
    if not picks:
        picks = ["friends"]
    return picks


def infer_group_size(companions: list[str], category: str) -> tuple[int, int]:
    min_size, max_size = 1, 4
    if "family" in companions:
        min_size, max_size = 3, 6
    elif "friends" in companions:
        min_size, max_size = 2, 6
    elif "couple" in companions:
        min_size, max_size = 2, 2
    if category in {"축제/행사", "쇼핑"}:
        max_size = max(max_size, 6)
    return min_size, max_size


def infer_transport_scores(text_blob: str, address: str) -> tuple[int, int]:
    pub_hits = count_keyword_hits(text_blob, PUBLIC_TRANSPORT_HINTS)
    car_hits = count_keyword_hits(text_blob, CAR_HINTS)
    district_boost = 1 if any(d in address for d in SEOUL_DISTRICTS) else 0

    public_score = min(100, 45 + pub_hits * 12 + district_boost * 8)
    car_score = min(100, 45 + car_hits * 12)

    if pub_hits == 0 and car_hits == 0:
        public_score = 55 + district_boost * 8
        car_score = 55

    return int(public_score), int(car_score)


def infer_confidence(search_count: int, crawled_count: int, text_len: int) -> float:
    base = 0.35
    base += min(0.25, search_count * 0.07)
    base += min(0.20, crawled_count * 0.07)
    base += min(0.20, text_len / 6000.0)
    return round(min(0.95, base), 3)


def enrich_record(record: SourceRecord) -> dict[str, Any]:
    base_query = f"\"{record.name}\" {record.address} 서울 여행 추천"
    raw_snippets = ddg_search_snippets(base_query, max_results=8)
    snippets = [s for s in raw_snippets if is_relevant_snippet(record, s)][:3]

    crawled_docs = []
    for s in snippets:
        href = s.get("href") or ""
        if not href:
            continue
        crawled = fetch_url_text(href)
        if crawled:
            crawled_docs.append({"url": href, "text": crawled[:1000]})
        time.sleep(0.2)

    if record.homepage:
        hp_text = fetch_url_text(record.homepage)
        if hp_text:
            crawled_docs.append({"url": record.homepage, "text": hp_text[:1000]})

    blob_parts = [
        record.name,
        record.address,
        record.content or "",
        " ".join([x.get("body", "") for x in snippets]),
        " ".join([x.get("text", "") for x in crawled_docs]),
    ]
    text_blob = " ".join(blob_parts)

    recommended_category = infer_category(
        text_blob,
        source_category_hint=record.source_category_hint or "",
    )
    companion_types = infer_companions(text_blob)
    min_group_size, max_group_size = infer_group_size(companion_types, recommended_category)
    public_score, car_score = infer_transport_scores(text_blob, record.address)
    confidence = infer_confidence(len(snippets), len(crawled_docs), len(text_blob))

    return {
        "source_type": record.source_type,
        "source_id": record.source_id,
        "name": record.name,
        "address": record.address,
        "recommended_category": recommended_category,
        "recommended_companion_types": companion_types,
        "min_group_size": min_group_size,
        "max_group_size": max_group_size,
        "score_transport": public_score,
        "score_car": car_score,
        "score_fit": confidence,
        "evidence": {
            "query": base_query,
            "snippets": snippets,
            "crawled": crawled_docs,
        },
    }


def load_attractions(conn: psycopg.Connection, limit: int) -> list[SourceRecord]:
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
            order by a.id desc
            limit %s
            """,
            (limit,),
        )
        rows = cur.fetchall()
    out = []
    for r in rows:
        meta = r.get("rag_metadata") or {}
        hint_parts = [
            r.get("category") or "",
            meta.get("cat1_name") or "",
            meta.get("cat2_name") or "",
            meta.get("cat3_name") or "",
            meta.get("content_type_name") or "",
        ]
        hint = " ".join(p for p in hint_parts if p)
        out.append(
            SourceRecord(
                source_type="attraction",
                source_id=str(r["id"]),
                name=r["name"] or "",
                address=r["address"] or "",
                content=f"{r['overview'] or ''} {r['category'] or ''}",
                source_category_hint=hint,
            )
        )
    return out


def load_events(conn: psycopg.Connection, limit: int) -> list[SourceRecord]:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            select
                t.id,
                t.content_id,
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
                  and source_id = coalesce(t.content_id, t.id)::text
                limit 1
            ) rd on true
            where t.language = 'KOR'
            order by t.id desc
            limit %s
            """,
            (limit,),
        )
        rows = cur.fetchall()
    out = []
    for r in rows:
        meta = r.get("rag_metadata") or {}
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


def write_json(path: str, records: list[dict[str, Any]]) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")


def _strip_nul_chars(value: Any) -> Any:
    if isinstance(value, str):
        return value.replace("\x00", "")
    if isinstance(value, list):
        return [_strip_nul_chars(v) for v in value]
    if isinstance(value, dict):
        return {k: _strip_nul_chars(v) for k, v in value.items()}
    return value


def upsert_results(conn: psycopg.Connection, table_name: str, rows: list[dict[str, Any]]) -> None:
    if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", table_name):
        raise ValueError("upsert table 이름이 안전하지 않습니다.")
    sql = f"""
        insert into {table_name} (
            source_type, source_id, name, address, recommended_category,
            recommended_companion_types, min_group_size, max_group_size,
            score_transport, score_car, score_fit, evidence, updated_at
        ) values (
            %(source_type)s, %(source_id)s, %(name)s, %(address)s, %(recommended_category)s,
            %(recommended_companion_types)s, %(min_group_size)s, %(max_group_size)s,
            %(score_transport)s, %(score_car)s, %(score_fit)s, %(evidence)s::jsonb, now()
        )
        on conflict (source_type, source_id) do update
        set
            name = excluded.name,
            address = excluded.address,
            recommended_category = excluded.recommended_category,
            recommended_companion_types = excluded.recommended_companion_types,
            min_group_size = excluded.min_group_size,
            max_group_size = excluded.max_group_size,
            score_transport = excluded.score_transport,
            score_car = excluded.score_car,
            score_fit = excluded.score_fit,
            evidence = excluded.evidence,
            updated_at = now()
    """
    with conn.cursor() as cur:
        for row in rows:
            cleaned_evidence = _strip_nul_chars(row["evidence"])
            payload = {
                "source_type": row["source_type"],
                "source_id": row["source_id"],
                "name": row["name"],
                "address": row["address"],
                "recommended_category": row["recommended_category"],
                "recommended_companion_types": row["recommended_companion_types"],
                "min_group_size": row["min_group_size"],
                "max_group_size": row["max_group_size"],
                "score_transport": row["score_transport"],
                "score_car": row["score_car"],
                "score_fit": row["score_fit"],
                "evidence": json.dumps(cleaned_evidence, ensure_ascii=False),
            }
            cur.execute(sql, payload)
    conn.commit()


def main() -> None:
    load_dotenv()

    parser = argparse.ArgumentParser(description="Supabase Postgres 직접연결 관광/행사 보강 지표 생성기")
    parser.add_argument("--source", choices=["attraction", "event", "both"], default="both")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--write-json", default="./out/enrichment.json")
    parser.add_argument("--upsert", action="store_true")
    parser.add_argument("--upsert-table", default=os.getenv("ENRICH_UPSERT_TABLE", "place_enrichment"))
    args = parser.parse_args()

    dsn = os.getenv("SUPABASE_DB_URL") or os.getenv("DATABASE_URL")
    db_host = os.getenv("DB_HOST")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME")
    db_user = os.getenv("SUPABASE_DB_USERNAME") or os.getenv("DB_USER")
    db_password = os.getenv("SUPABASE_DB_PASSWORD") or os.getenv("DB_PASSWORD")

    if dsn:
        conn = psycopg.connect(dsn)
    else:
        if not all([db_host, db_name, db_user, db_password]):
            raise RuntimeError(
                "SUPABASE_DB_URL 또는 SUPABASE_DB_USERNAME/SUPABASE_DB_PASSWORD(+DB_HOST/DB_NAME) 환경변수를 설정하세요."
            )
        conn = psycopg.connect(
            host=db_host,
            port=db_port,
            dbname=db_name,
            user=db_user,
            password=db_password,
            sslmode=os.getenv("DB_SSLMODE", "require"),
        )

    records: list[SourceRecord] = []
    if args.source in {"attraction", "both"}:
        records.extend(load_attractions(conn, args.limit))
    if args.source in {"event", "both"}:
        records.extend(load_events(conn, args.limit))

    enriched = [enrich_record(r) for r in records if r.name]
    write_json(args.write_json, enriched)

    if args.upsert and enriched:
        upsert_results(conn, args.upsert_table, enriched)

    conn.close()

    print(f"[DONE] source={args.source}, input={len(records)}, enriched={len(enriched)}")
    print(f"[JSON] {args.write_json}")
    if args.upsert:
        print(f"[UPSERT] table={args.upsert_table}")


if __name__ == "__main__":
    main()
