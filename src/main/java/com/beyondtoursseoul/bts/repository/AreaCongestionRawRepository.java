package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.AreaCongestionRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AreaCongestionRawRepository extends JpaRepository<AreaCongestionRaw, Long> {

    boolean existsByAreaCodeAndPopulationTime(String areaCode, LocalDateTime populationTime);

    Optional<AreaCongestionRaw> findByAreaCodeAndPopulationTime(String areaCode, LocalDateTime populationTime);

    List<AreaCongestionRaw> findTop100ByOrderByCollectedAtDesc();

    List<AreaCongestionRaw> findByAreaCodeOrderByPopulationTimeDesc(String areaCode);
}
