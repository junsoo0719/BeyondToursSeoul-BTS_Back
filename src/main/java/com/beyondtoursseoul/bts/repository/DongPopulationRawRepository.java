package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.DongPopulationRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DongPopulationRawRepository extends JpaRepository<DongPopulationRaw, Long> {

    boolean existsByDate(LocalDate date);

    List<DongPopulationRaw> findByDateAndTimeSlot(LocalDate date, String timeSlot);

    List<DongPopulationRaw> findByTimeSlotAndDateBetween(String timeSlot, LocalDate from, LocalDate to);
}