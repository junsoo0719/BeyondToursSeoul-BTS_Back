package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.course.TourCourseItemTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TourCourseItemTranslationRepository extends JpaRepository<TourCourseItemTranslation, Long> {

    @Query("SELECT t FROM TourCourseItemTranslation t JOIN FETCH t.courseItem "
            + "WHERE t.courseItem.id IN :ids AND t.language = :lang")
    List<TourCourseItemTranslation> findByCourseItem_IdInAndLanguage(
            @Param("ids") Collection<Long> ids,
            @Param("lang") TourLanguage lang);
}
