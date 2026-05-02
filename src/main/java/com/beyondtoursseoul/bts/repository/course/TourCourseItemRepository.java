package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.course.TourCourse;
import com.beyondtoursseoul.bts.domain.course.TourCourseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourCourseItemRepository extends JpaRepository<TourCourseItem, Long> {
    List<TourCourseItem> findByCourseOrderBySequenceOrderAsc(TourCourse course);
}
