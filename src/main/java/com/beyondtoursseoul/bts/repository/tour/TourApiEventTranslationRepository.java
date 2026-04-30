package com.beyondtoursseoul.bts.repository.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourApiEventTranslationRepository extends JpaRepository<TourApiEventTranslation, Long> {

    // 특정 행사, 언어에 해당하는 번역 데이터가 있는지 확인
    Optional<TourApiEventTranslation> findByEventAndLanguage(TourApiEvent event, TourLanguage language);
}
