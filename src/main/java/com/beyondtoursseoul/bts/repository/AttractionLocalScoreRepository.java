package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.domain.AttractionLocalScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttractionLocalScoreRepository extends JpaRepository<AttractionLocalScore, AttractionLocalScoreId> {

    @Query("SELECT MAX(a.id.date) FROM AttractionLocalScore a")
    Optional<LocalDate> findLatestDate();

    List<AttractionLocalScore> findByIdAttractionIdAndIdDate(Long attractionId, LocalDate date);

    List<AttractionLocalScore> findByIdDateAndIdTimeSlot(LocalDate date, String timeSlot);

    @Query("SELECT s FROM AttractionLocalScore s WHERE s.id.date = :date AND s.id.timeSlot = :timeSlot AND s.id.attractionId IN :attractionIds")
    List<AttractionLocalScore> findByDateAndTimeSlotAndAttractionIdIn(
            @Param("date") LocalDate date,
            @Param("timeSlot") String timeSlot,
            @Param("attractionIds") Collection<Long> attractionIds);
}
