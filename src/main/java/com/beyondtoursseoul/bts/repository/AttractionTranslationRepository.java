package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.AttractionTranslationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttractionTranslationRepository extends JpaRepository<AttractionTranslation, AttractionTranslationId> {

    Optional<AttractionTranslation> findByIdAttractionIdAndIdLang(Long attractionId, String lang);

    List<AttractionTranslation> findByIdAttractionIdInAndIdLang(List<Long> attractionIds, String lang);
}
