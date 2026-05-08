package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.AreaCongestionRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AreaCongestionRawRepository extends JpaRepository<AreaCongestionRaw, Long> {

    Optional<AreaCongestionRaw> findByAreaCode(String areaCode);
}
