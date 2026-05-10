package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findByDetailFetchedFalseAndExternalIdNotNull();

    /**
     * 목록용: 해당 일자·시간대에 점수가 있고, 점수 구간을 만족하는 관광지만 조인으로 조회한다.
     * (전체 attraction findAll 후 메모리 필터링 대신 DB에서 걸러 이그레스·메모리를 줄인다.)
     */
    @Query("""
            select a, s from Attraction a
            join AttractionLocalScore s on s.id.attractionId = a.id
            where s.id.date = :date
              and s.id.timeSlot = :timeSlot
              and (:minScore is null or s.score >= :minScore)
              and (:maxScore is null or s.score <= :maxScore)
            order by s.score desc nulls last
            """)
    List<Object[]> findWithLocalScoresForList(
            @Param("date") LocalDate date,
            @Param("timeSlot") String timeSlot,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore
    );

    // 카테고리 필터(다국어 및 대소문자 무시 지원)와 페이지네이션이 적용
    @Query("""
            select a, s from Attraction a
            join AttractionLocalScore s on s.id.attractionId = a.id
            where s.id.date = :date
              and s.id.timeSlot = :timeSlot
              and (:minScore is null or s.score >= :minScore)
              and (:maxScore is null or s.score <= :maxScore)
              and (:hasCategory = false or exists (
                  select 1 from TourCategory c 
                  where (c.code = a.cat1 or c.code = a.cat2 or c.code = a.cat3)
                    and (c.name like :categoryKeyword 
                      or LOWER(c.nameEn) like LOWER(:categoryKeyword) 
                      or c.nameZh like :categoryKeyword 
                      or c.nameJa like :categoryKeyword)
              ))
            order by s.score desc nulls last
            """)
    Page<Object[]> findWithLocalScoresPage(
            @Param("date") LocalDate date,
            @Param("timeSlot") String timeSlot,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            @Param("hasCategory") boolean hasCategory,
            @Param("categoryKeyword") String categoryKeyword,
            Pageable pageable
    );
}
