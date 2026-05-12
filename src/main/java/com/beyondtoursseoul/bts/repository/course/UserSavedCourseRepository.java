package com.beyondtoursseoul.bts.repository.course;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.course.TourCourse;
import com.beyondtoursseoul.bts.domain.course.UserSavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserSavedCourseRepository extends JpaRepository<UserSavedCourse, Long> {
    List<UserSavedCourse> findByUserOrderBySavedAtDesc(Profile user);
    Optional<UserSavedCourse> findByUserAndCourse(Profile user, TourCourse course);
    boolean existsByUserAndCourse(Profile user, TourCourse course);

    @Query("SELECT DISTINCT usc.course.id FROM UserSavedCourse usc WHERE usc.user.id = :userId")
    Set<Long> findSavedCourseIdsByUserId(@Param("userId") UUID userId);
}
