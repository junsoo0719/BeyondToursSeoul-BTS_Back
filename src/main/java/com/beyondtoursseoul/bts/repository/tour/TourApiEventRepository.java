package com.beyondtoursseoul.bts.repository.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
