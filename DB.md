-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

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
CREATE TABLE public.cultural_event (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  category_code character varying,
  category_depth character varying,
  created_at timestamp with time zone,
  end_date date,
  homepage_url character varying,
  is_free boolean,
  latitude double precision NOT NULL,
  longitude double precision NOT NULL,
  main_img_url character varying,
  master_cid character varying NOT NULL UNIQUE,
  start_date date,
  tel_no character varying,
  updated_at timestamp with time zone,
  CONSTRAINT cultural_event_pkey PRIMARY KEY (id)
);
CREATE TABLE public.cultural_event_image (
  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  image_url character varying NOT NULL,
  cultural_event_id bigint,
  CONSTRAINT cultural_event_image_pkey PRIMARY KEY (id),
  CONSTRAINT fkitqyf3ra6nvdh19qqnh73ndbs FOREIGN KEY (cultural_event_id) REFERENCES public.cultural_event(id)
);
CREATE TABLE public.cultural_event_translation (
  id bigint NOT NULL DEFAULT nextval('cultural_event_translation_id_seq'::regclass),
  cultural_event_id bigint,
  cid character varying NOT NULL,
  lang_code character varying NOT NULL,
  title character varying NOT NULL,
  summary text,
  post_desc text,
  address character varying,
  use_time text,
  fee_info text,
  closed_days character varying,
  traffic_info text,
  tags text,
  CONSTRAINT cultural_event_translation_pkey PRIMARY KEY (id),
  CONSTRAINT cultural_event_translation_cultural_event_id_fkey FOREIGN KEY (cultural_event_id) REFERENCES public.cultural_event(id)
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
  CONSTRAINT tour_category_pkey PRIMARY KEY (code)
);