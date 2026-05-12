package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.course.CourseItemType;
import com.beyondtoursseoul.bts.domain.course.TourCourse;
import com.beyondtoursseoul.bts.domain.course.TourCourseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TourCourseItemRepository extends JpaRepository<TourCourseItem, Long> {
    List<TourCourseItem> findByCourseOrderBySequenceOrderAsc(TourCourse course);

    @Query("SELECT DISTINCT i FROM TourCourseItem i "
            + "LEFT JOIN FETCH i.attraction "
            + "LEFT JOIN FETCH i.event "
            + "WHERE i.course = :course "
            + "ORDER BY i.sequenceOrder ASC")
    List<TourCourseItem> findByCourseWithSpotsFetchedOrderBySequence(@Param("course") TourCourse course);

    @Query("SELECT i FROM TourCourseItem i JOIN FETCH i.attraction WHERE i.course.id IN :courseIds AND i.itemType = :itemType")
    List<TourCourseItem> findByCourseIdInWithAttractionFetched(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("itemType") CourseItemType itemType);

    @Query("SELECT i FROM TourCourseItem i "
            + "LEFT JOIN FETCH i.attraction "
            + "LEFT JOIN FETCH i.event "
            + "WHERE i.course.id IN :courseIds "
            + "ORDER BY i.course.id ASC, i.sequenceOrder ASC")
    List<TourCourseItem> findByCourseIdInWithSpotsFetchedOrdered(@Param("courseIds") Collection<Long> courseIds);
}
