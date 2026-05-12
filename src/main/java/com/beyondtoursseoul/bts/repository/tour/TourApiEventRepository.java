package com.beyondtoursseoul.bts.repository.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TourApiEventRepository extends JpaRepository<TourApiEvent, Long> {

    /**
     * 특정 언어에 대해 번역이 누락되었거나, 자동 번역본이 최신이 아닌(원본이 수정된) 이벤트 목록을 조회합니다.
     */
    @Query("SELECT e FROM TourApiEvent e " +
            "LEFT JOIN e.translations t ON t.language = :lang " +
            "WHERE t IS NULL OR (t.isAutoTranslated = true AND t.lastTranslatedModifiedTime <> e.modifiedTime)")
    List<TourApiEvent> findEventsNeedingTranslation(@Param("lang") TourLanguage lang);

    /**
     * 특정 기준일(오늘)을 포함하여 종료되지 않은 진행 중이거나 예정된 행사 목록을 조회합니다.
     */
    @Query("SELECT e FROM TourApiEvent e WHERE e.eventEndDate >= :today")
    List<TourApiEvent> findValidEvents(@Param("today") String today);

    /**
     * 페이징 적용 리스트 조회
     */
    @Query("SELECT e FROM TourApiEvent e WHERE e.eventEndDate >= :today")
    Page<TourApiEvent> findValidEventsPage(@Param("today") String today, Pageable pageable);

    /**
     * 기준점에서 가장 가까운 '아직 종료되지 않은' 행사(content_id). 좌표가 있는 행사만.
     * PostGIS geography 거리(m) 기준.
     */
    @Query(value = """
            SELECT e.content_id FROM tour_api_event e
            WHERE e.map_x IS NOT NULL AND e.map_y IS NOT NULL
              AND e.event_end_date IS NOT NULL AND e.event_end_date >= :today
              AND e.content_id NOT IN (:excludeIds)
            ORDER BY ST_Distance(
                ST_SetSRID(ST_MakePoint(e.map_x, e.map_y), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            )
            LIMIT 1
            """, nativeQuery = true)
    List<Long> findNearestValidEventContentIdExcluding(
            @Param("lon") double lon,
            @Param("lat") double lat,
            @Param("today") String today,
            @Param("excludeIds") Collection<Long> excludeIds);
}
