# Supabase Postgres 직접연결 관광/행사 데이터 보강 모듈

이 모듈은 백엔드(Spring)와 독립적으로 실행되는 Python 스크립트입니다.

## 기능
- Supabase Postgres에 직접 연결해서 관광지(`attraction`) + 행사(`tour_api_event_translation`) 조회
- 웹 검색(DDG) + 페이지 본문 일부 크롤링
- 규칙 기반 지표 산출
  - 추천 카테고리
  - 추천 동행 유형
  - 권장 인원(최소/최대)
  - 대중교통 지수 vs 자동차 지수
- 결과를 JSON으로 저장
- 선택적으로 Supabase 테이블에 upsert

현재 추천 카테고리 라벨은 아래 12개로 고정되어 있습니다.
- 카페
- 맛집
- 역사/문화
- 자연
- 예술/전시
- K-팝/아이돌
- 쇼핑
- 액티비티
- 힐링
- 야경/바
- 포토스팟
- 로컬 탐방

## 1) 설치
```bash
cd tools/data_enrichment
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## 2) 환경변수
`.env` 또는 시스템 환경변수에 아래를 설정하세요.

- 권장(한 줄): `SUPABASE_DB_URL` (또는 `DATABASE_URL`)
  - 예: `postgresql://USER:PASSWORD@HOST:5432/postgres?sslmode=require`

또는 개별 설정:
- `DB_HOST`
- `DB_PORT` (기본 `5432`)
- `DB_NAME`
- `SUPABASE_DB_USERNAME` (또는 `DB_USER`)
- `SUPABASE_DB_PASSWORD` (또는 `DB_PASSWORD`)
- `DB_SSLMODE` (기본 `require`)

옵션:
- `ENRICH_UPSERT_TABLE` (예: `place_enrichment`)

## 3) 실행
```bash
python enrich_supabase_data.py --source both --limit 100 --write-json ./out/enrichment.json
```

upsert까지:
```bash
python enrich_supabase_data.py --source both --limit 100 --upsert --upsert-table place_enrichment
```

## 4) 권장 테이블 스키마(예시)
```sql
create table if not exists place_enrichment (
  source_type text not null,
  source_id text not null,
  name text,
  address text,
  recommended_category text,
  recommended_companion_types text[] default '{}',
  min_group_size int,
  max_group_size int,
  score_transport int,
  score_car int,
  score_fit numeric,
  evidence jsonb,
  updated_at timestamptz default now(),
  primary key (source_type, source_id)
);
```

## 주의
- 검색/크롤링은 외부 사이트 정책에 따라 일부 실패할 수 있습니다.
- 현재는 규칙 기반 스코어링입니다. 이후 LLM 재랭킹 단계 추가를 권장합니다.
