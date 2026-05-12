package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.course.TourCourse;
import com.beyondtoursseoul.bts.domain.course.TourCourseTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourCourseTranslationRepository extends JpaRepository<TourCourseTranslation, Long> {
    Optional<TourCourseTranslation> findByCourseAndLanguage(TourCourse course, TourLanguage language);

    List<TourCourseTranslation> findByCourse_IdIn(Collection<Long> courseIds);
}
