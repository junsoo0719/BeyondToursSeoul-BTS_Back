-- Manual DDL for the first Seoul citydata congestion collection schema.
-- Apply this in Supabase SQL Editor (or PostgreSQL) before running
-- the area congestion sample collector with ddl-auto=none.

create table if not exists public.area_congestion_raw (
    id bigserial primary key,
    area_code varchar(50) not null,
    area_name varchar(100) not null,
    congestion_level varchar(50) not null,
    latitude double precision not null,
    longitude double precision not null,
    population_time timestamp not null,
    collected_at timestamptz not null,
    constraint uk_area_congestion_raw_area_code
        unique (area_code)
);
