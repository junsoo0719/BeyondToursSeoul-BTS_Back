-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.area_congestion_raw (
  id bigint NOT NULL DEFAULT nextval('area_congestion_raw_id_seq'::regclass),
  area_code character varying NOT NULL UNIQUE,
  area_name character varying NOT NULL,
  congestion_level character varying NOT NULL,
  population_time timestamp without time zone NOT NULL,
  collected_at timestamp with time zone NOT NULL,
  latitude double precision,
  longitude double precision,
  CONSTRAINT area_congestion_raw_pkey PRIMARY KEY (id)
);
CREATE TABLE public.attraction (
  id bigint NOT NULL DEFAULT nextval('attraction_id_seq'::regclass),
  external_id character varying,
  name character varying,
  category character varying,
  address character varying,
  geom USER-DEFINED,
  dong_code character varying,
  source character varying,
  created_at timestamp with time zone,
  thumbnail character varying,
  cat1 character varying,
  cat2 character varying,
  cat3 character varying,
  tel text,
  overview text,
  operating_hours text,
  detail_fetched boolean NOT NULL DEFAULT false,
  CONSTRAINT attraction_pkey PRIMARY KEY (id),
  CONSTRAINT fk_attraction_dong FOREIGN KEY (dong_code) REFERENCES public.dong_boundary(dong_code)
);
CREATE TABLE public.attraction_local_score (
  attraction_id bigint NOT NULL,
  date date NOT NULL,
  time_slot character varying NOT NULL,
  score numeric,
  hour integer NOT NULL,
  CONSTRAINT attraction_local_score_pkey PRIMARY KEY (attraction_id, date, time_slot),
  CONSTRAINT fk_als_attraction FOREIGN KEY (attraction_id) REFERENCES public.attraction(id)
);
CREATE TABLE public.attraction_translation (
  attraction_id bigint NOT NULL,
  lang character varying NOT NULL,
  name text,
  address text,
  overview text,
  operating_hours text,
  CONSTRAINT attraction_translation_pkey PRIMARY KEY (attraction_id, lang),
  CONSTRAINT attraction_translation_attraction_id_fkey FOREIGN KEY (attraction_id) REFERENCES public.attraction(id)
);
CREATE TABLE public.dong_boundary (
  dong_code character varying NOT NULL,
  dong_name character varying,
  geom USER-DEFINED,
  CONSTRAINT dong_boundary_pkey PRIMARY KEY (dong_code)
);
CREATE TABLE public.dong_local_score (
  id bigint NOT NULL DEFAULT nextval('dong_local_score_id_seq'::regclass),
  dong_code character varying,
  date date,
  time_slot character varying,
  score numeric,
  breakdown_json text,
  hour integer,
  CONSTRAINT dong_local_score_pkey PRIMARY KEY (id)
);
CREATE TABLE public.dong_population_raw (
  id bigint NOT NULL DEFAULT nextval('dong_population_raw_id_seq'::regclass),
  dong_code character varying NOT NULL,
  date date NOT NULL,
  time_slot character varying NOT NULL,
  korean_pop numeric NOT NULL,
  foreign_pop numeric NOT NULL,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT dong_population_raw_pkey PRIMARY KEY (id)
);
CREATE TABLE public.locker_translations (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  add_price_info text,
  base_price_info text,
  detail_location character varying,
  language_code character varying NOT NULL,
  limit_items_info text,
  locker_name character varying,
  size_info character varying,
  station_name character varying,
  locker_id bigint NOT NULL,
  CONSTRAINT locker_translations_pkey PRIMARY KEY (id),
  CONSTRAINT fk4m1c32by5gmqn8f2vyx4vrwbs FOREIGN KEY (locker_id) REFERENCES public.lockers(id)
);
CREATE TABLE public.lockers (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  add_charge_unit integer,
  latitude double precision NOT NULL,
  lckr_id character varying NOT NULL UNIQUE,
  longitude double precision NOT NULL,
  total_cnt integer,
  weekday_end_time character varying,
  weekday_start_time character varying,
  weekend_end_time character varying,
  weekend_start_time character varying,
  CONSTRAINT lockers_pkey PRIMARY KEY (id)
);
CREATE TABLE public.place_enrichment (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  source_type character varying NOT NULL,
  source_id character varying NOT NULL,
  name character varying,
  address character varying,
  recommended_category character varying NOT NULL DEFAULT '기타'::character varying,
  recommended_companion_types ARRAY NOT NULL DEFAULT ARRAY['friends'::text],
  min_group_size integer NOT NULL DEFAULT 1,
  max_group_size integer NOT NULL DEFAULT 4,
  score_transport integer NOT NULL DEFAULT 55 CHECK (score_transport >= 0 AND score_transport <= 100),
  score_car integer NOT NULL DEFAULT 55 CHECK (score_car >= 0 AND score_car <= 100),
  score_fit numeric NOT NULL DEFAULT 0.350 CHECK (score_fit >= 0::numeric AND score_fit <= 1::numeric),
  evidence jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT place_enrichment_pkey PRIMARY KEY (id)
);
CREATE TABLE public.profiles (
  id uuid NOT NULL,
  nickname character varying,
  preferred_language character varying,
  visit_count integer DEFAULT 0,
  local_preference character varying,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT profiles_pkey PRIMARY KEY (id),
  CONSTRAINT profiles_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id)
);
CREATE TABLE public.rag_documents (
  id bigint NOT NULL DEFAULT nextval('rag_documents_id_seq'::regclass),
  source_type text NOT NULL,
  source_id text NOT NULL,
  title text,
  content text NOT NULL,
  lang_code text,
  dong_code text,
  latitude double precision,
  longitude double precision,
  metadata jsonb,
  embedding USER-DEFINED,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT rag_documents_pkey PRIMARY KEY (id)
);
CREATE TABLE public.tour_api_event (
  content_id bigint NOT NULL,
  content_type_id bigint,
  event_end_date character varying,
  event_start_date character varying,
  first_image character varying,
  first_image2 character varying,
  last_sync_time timestamp without time zone,
  map_x double precision,
  map_y double precision,
  modified_time character varying,
  tel character varying,
  zipcode character varying,
  CONSTRAINT tour_api_event_pkey PRIMARY KEY (content_id)
);
CREATE TABLE public.tour_api_event_image (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  copyright_type character varying,
  origin_img_url character varying,
  small_img_url character varying,
  content_id bigint,
  CONSTRAINT tour_api_event_image_pkey PRIMARY KEY (id),
  CONSTRAINT fk4jc6mrb4jjxc9iipy2wwovikp FOREIGN KEY (content_id) REFERENCES public.tour_api_event(content_id)
);
CREATE TABLE public.tour_api_event_translation (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  address character varying,
  age_limit character varying,
  booking_place character varying,
  discount_info_festival character varying,
  event_place character varying,
  festival_grade character varying,
  homepage character varying,
  is_auto_translated boolean,
  language character varying CHECK (language::text = ANY (ARRAY['KOR'::character varying, 'ENG'::character varying, 'JPN'::character varying, 'CHS'::character varying, 'CHT'::character varying]::text[])),
  last_translated_modified_time character varying,
  overview text,
  play_time character varying,
  program text,
  spend_time_festival character varying,
  sponsor1 character varying,
  sponsor1tel character varying,
  sponsor2 character varying,
  sponsor2tel character varying,
  sub_event character varying,
  tel_name character varying,
  title character varying NOT NULL,
  use_time_festival character varying,
  content_id bigint,
  CONSTRAINT tour_api_event_translation_pkey PRIMARY KEY (id),
  CONSTRAINT fkbov4994e38ba7viqmpmtmb91v FOREIGN KEY (content_id) REFERENCES public.tour_api_event(content_id)
);
CREATE TABLE public.tour_category (
  code character varying NOT NULL,
  name character varying NOT NULL,
  level integer NOT NULL,
  name_en character varying,
  name_zh character varying,
  name_ja character varying,
  CONSTRAINT tour_category_pkey PRIMARY KEY (code)
);
CREATE TABLE public.tour_course_items (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  ai_comment text,
  item_type character varying NOT NULL CHECK (item_type::text = ANY (ARRAY['ATTRACTION'::character varying, 'EVENT'::character varying]::text[])),
  sequence_order integer NOT NULL,
  attraction_id bigint,
  course_id bigint NOT NULL,
  event_id bigint,
  CONSTRAINT tour_course_items_pkey PRIMARY KEY (id),
  CONSTRAINT fkho3ueb589vcxh6qri9hd0gfll FOREIGN KEY (attraction_id) REFERENCES public.attraction(id),
  CONSTRAINT fklxnvbsp615lv9nq52wo0jujn5 FOREIGN KEY (course_id) REFERENCES public.tour_courses(id),
  CONSTRAINT fki9mb3c9soa8qsggtxdv03n5hu FOREIGN KEY (event_id) REFERENCES public.tour_api_event(content_id)
);
-- 코스 스텝별 ai_comment 다국어 (없으면 tour_course_items.ai_comment 한국어 사용)
CREATE TABLE public.tour_course_item_translations (
  id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  course_item_id bigint NOT NULL,
  language character varying(10) NOT NULL CHECK (language::text = ANY (ARRAY['KOR'::character varying, 'ENG'::character varying, 'JPN'::character varying, 'CHS'::character varying, 'CHT'::character varying]::text[])),
  ai_comment text NOT NULL,
  CONSTRAINT tour_course_item_translations_pkey PRIMARY KEY (id),
  CONSTRAINT tour_course_item_translations_item_fkey FOREIGN KEY (course_item_id) REFERENCES public.tour_course_items(id) ON DELETE CASCADE,
  CONSTRAINT tour_course_item_translations_item_lang_uq UNIQUE (course_item_id, language)
);
CREATE INDEX idx_tour_course_item_tr_item ON public.tour_course_item_translations(course_item_id);
CREATE TABLE public.tour_course_translations (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  hashtags character varying,
  language character varying NOT NULL CHECK (language::text = ANY (ARRAY['KOR'::character varying, 'ENG'::character varying, 'JPN'::character varying, 'CHS'::character varying, 'CHT'::character varying]::text[])),
  title character varying NOT NULL,
  course_id bigint NOT NULL,
  CONSTRAINT tour_course_translations_pkey PRIMARY KEY (id),
  CONSTRAINT fkbis93f7dgawlphjosb5mh8cfk FOREIGN KEY (course_id) REFERENCES public.tour_courses(id)
);
CREATE TABLE public.tour_courses (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  created_at timestamp without time zone,
  featured_image character varying,
  hashtags character varying,
  title character varying NOT NULL,
  CONSTRAINT tour_courses_pkey PRIMARY KEY (id)
);
CREATE TABLE public.user_saved_courses (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  saved_at timestamp without time zone,
  course_id bigint NOT NULL,
  user_id uuid NOT NULL,
  CONSTRAINT user_saved_courses_pkey PRIMARY KEY (id),
  CONSTRAINT fkepx2ib9v2ro1banjvs29y6oup FOREIGN KEY (course_id) REFERENCES public.tour_courses(id),
  CONSTRAINT fkilwyad89p8e78y34uxvrbd6oy FOREIGN KEY (user_id) REFERENCES public.profiles(id)
);

-- ---------------------------------------------------------------------------
-- 개인화 저장함 (Supabase 등에 아래 DDL을 적용한 뒤 API 사용)
-- - user_saved_courses: 공식 추천 코스(tour_courses) 저장 — 기존
-- - user_saved_attractions: 관광지(attraction) 즐겨찾기
-- - user_saved_events: 행사(tour_api_event) 즐겨찾기
-- - user_saved_plans: AI/챗봇 일정 등 JSON 구조 전체 저장 (프론트 structured 그대로)
-- ---------------------------------------------------------------------------

CREATE TABLE public.user_saved_attractions (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  user_id uuid NOT NULL,
  attraction_id bigint NOT NULL,
  saved_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_saved_attractions_pkey PRIMARY KEY (id),
  CONSTRAINT user_saved_attractions_user_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE,
  CONSTRAINT user_saved_attractions_attraction_fkey FOREIGN KEY (attraction_id) REFERENCES public.attraction(id) ON DELETE CASCADE,
  CONSTRAINT user_saved_attractions_user_attraction_unique UNIQUE (user_id, attraction_id)
);
CREATE INDEX idx_user_saved_attractions_user_saved_at ON public.user_saved_attractions (user_id, saved_at DESC);

CREATE TABLE public.user_saved_events (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  user_id uuid NOT NULL,
  event_content_id bigint NOT NULL,
  saved_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_saved_events_pkey PRIMARY KEY (id),
  CONSTRAINT user_saved_events_user_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE,
  CONSTRAINT user_saved_events_event_fkey FOREIGN KEY (event_content_id) REFERENCES public.tour_api_event(content_id) ON DELETE CASCADE,
  CONSTRAINT user_saved_events_user_event_unique UNIQUE (user_id, event_content_id)
);
CREATE INDEX idx_user_saved_events_user_saved_at ON public.user_saved_events (user_id, saved_at DESC);

CREATE TABLE public.user_saved_plans (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  user_id uuid NOT NULL,
  title character varying(500),
  structured_json text NOT NULL,
  saved_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_saved_plans_pkey PRIMARY KEY (id),
  CONSTRAINT user_saved_plans_user_fkey FOREIGN KEY (user_id) REFERENCES public.profiles(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_saved_plans_user_saved_at ON public.user_saved_plans (user_id, saved_at DESC);

-- API (JWT 필수)
-- GET    /api/v1/me/saved/attractions
-- POST   /api/v1/me/saved/attractions/{attractionId}  → { "saved": true|false }
-- GET    /api/v1/me/saved/events
-- POST   /api/v1/me/saved/events/{contentId}  → { "saved": true|false }
-- GET    /api/v1/me/saved/plans
-- POST   /api/v1/me/saved/plans  body: { "title"?: string, "structured": { ... } }
-- GET    /api/v1/me/saved/plans/{planId}
-- DELETE /api/v1/me/saved/plans/{planId}
-- 공식 코스 저장(기존): POST /api/v1/courses/{courseId}/save , GET /api/v1/courses/saved