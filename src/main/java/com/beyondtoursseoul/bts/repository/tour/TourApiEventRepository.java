package com.beyondtoursseoul.bts.repository.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourApiEventRepository extends JpaRepository<TourApiEvent, Long> {

}
