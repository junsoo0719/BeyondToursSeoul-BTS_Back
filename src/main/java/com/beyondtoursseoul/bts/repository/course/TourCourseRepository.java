package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.course.TourCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourCourseRepository extends JpaRepository<TourCourse, Long> {
}
