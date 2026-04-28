package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.DongLocalScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DongLocalScoreRepository extends JpaRepository<DongLocalScore, Long> {

    boolean existsByDongCodeAndDateAndTimeSlot(String dongCode, LocalDate date, String timeSlot);

    List<DongLocalScore> findByDate(LocalDate date);

    void deleteByDateAndTimeSlot(LocalDate date, String timeSlot);
}
