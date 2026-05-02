package com.beyondtoursseoul.bts.service.score;

import com.beyondtoursseoul.bts.repository.AttractionLocalScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionScoreService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AttractionLocalScoreRepository repository;

    // 카테고리별 보정 계수 — 관광객 유입 가능성이 높은 카테고리는 하향
    private static final String CATEGORY_FACTOR_CASE = """
            CASE a.category
                WHEN '12' THEN 0.85
                WHEN '14' THEN 0.85
                WHEN '32' THEN 0.75
                ELSE 1.0
            END
            """;

    // 주요 관광 클러스터 (lng, lat) — 명동/인사동/홍대/이태원/남산/경복궁/동대문/코엑스
    private static final String TOURIST_CLUSTERS_VALUES = """
            (126.9836, 37.5635),
            (126.9851, 37.5745),
            (126.9218, 37.5565),
            (126.9944, 37.5344),
            (126.9882, 37.5512),
            (126.9770, 37.5796),
            (127.0095, 37.5710),
            (127.0590, 37.5113)
            """;

    // 동시 실행 방지용 advisory lock 키 (임의 고정 값)
    private static final long ADVISORY_LOCK_KEY = 20260101L;

    public void calculateAndSave(LocalDate date) {
        boolean alreadyDone = repository.findLatestDate()
                .map(latest -> !latest.isBefore(date))
                .orElse(false);
        if (alreadyDone) {
            log.info("[AttractionScore] {} 이미 계산됨 — 스킵", date);
            return;
        }

        // 이전 세션이 아직 INSERT 중이면 즉시 포기 (데드락 방지)
        Boolean locked = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(?)", Boolean.class, ADVISORY_LOCK_KEY);
        if (!Boolean.TRUE.equals(locked)) {
            log.warn("[AttractionScore] 다른 세션이 계산 중 — 스킵 (date={})", date);
            return;
        }

        try {
            int total = 0;
            for (TimeSlot timeSlot : TimeSlot.values()) {
                int count = insertForTimeSlot(date, timeSlot);
                total += count;
                log.info("[AttractionScore] {} {} — {}건 저장", date, timeSlot.getCode(), count);
            }
            log.info("[AttractionScore] 완료: {} 총 {}건", date, total);
        } finally {
            jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, ADVISORY_LOCK_KEY);
        }
    }

    private int insertForTimeSlot(LocalDate date, TimeSlot timeSlot) {
        String insertSql = """
                INSERT INTO attraction_local_score (attraction_id, date, time_slot, hour, score)
                SELECT a.id, ?, ?, ?,
                    LEAST(1.0, GREATEST(0.0,
                        (SUM(s.score / GREATEST(ST_Distance(a.geom::geography, ST_Centroid(d.geom)::geography), 1.0)^2)
                         / SUM(1.0   / GREATEST(ST_Distance(a.geom::geography, ST_Centroid(d.geom)::geography), 1.0)^2))
                        * (%s)
                        * MAX(tc.proximity_factor)
                    ))
                FROM attraction a
                JOIN dong_boundary d ON ST_DWithin(a.geom::geography, d.geom::geography, 1000)
                JOIN dong_local_score s ON s.dong_code = d.dong_code,
                LATERAL (
                    SELECT 0.5 + 0.5 * (1 - EXP(-MIN(ST_Distance(
                            a.geom::geography,
                            ST_SetSRID(ST_MakePoint(c.lng, c.lat), 4326)::geography
                        )) / 2000.0)) AS proximity_factor
                    FROM (VALUES %s) AS c(lng, lat)
                ) tc
                WHERE s.date = ? AND s.time_slot = ?
                  AND d.geom IS NOT NULL
                GROUP BY a.id
                """.formatted(CATEGORY_FACTOR_CASE, TOURIST_CLUSTERS_VALUES);

        Integer count = transactionTemplate.execute(status -> {
            jdbcTemplate.update(
                    "DELETE FROM attraction_local_score WHERE date = ? AND time_slot = ?",
                    date, timeSlot.getCode());
            return jdbcTemplate.update(insertSql,
                    date, timeSlot.getCode(), timeSlot.getStartHour(), date, timeSlot.getCode());
        });
        return count != null ? count : 0;
    }

    // Approach C: contentTypeId 기준 대표 시간대
    public static TimeSlot primaryTimeSlot(String contentTypeId) {
        if (contentTypeId == null) return TimeSlot.AFTERNOON;
        return switch (contentTypeId) {
            case "39" -> TimeSlot.EVENING;    // 음식점
            case "15" -> TimeSlot.EVENING;    // 행사/공연/축제
            case "28" -> TimeSlot.MORNING;    // 레포츠
            case "32" -> TimeSlot.MORNING;    // 숙박
            default   -> TimeSlot.AFTERNOON;  // 관광지(12), 문화시설(14), 쇼핑(38) 등
        };
    }
}
