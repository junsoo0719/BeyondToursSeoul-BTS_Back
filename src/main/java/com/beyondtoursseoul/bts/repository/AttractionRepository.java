package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findByDetailFetchedFalseAndExternalIdNotNull();
}
