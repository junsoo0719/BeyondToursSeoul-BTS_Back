package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourCategoryRepository extends JpaRepository<TourCategory, String> {
}
