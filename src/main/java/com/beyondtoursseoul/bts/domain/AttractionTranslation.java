package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attraction_translation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttractionTranslation {

    @EmbeddedId
    private AttractionTranslationId id;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "operating_hours", columnDefinition = "TEXT")
    private String operatingHours;

    @Builder
    public AttractionTranslation(Long attractionId, String lang,
                                 String name, String address,
                                 String overview, String operatingHours) {
        this.id = new AttractionTranslationId(attractionId, lang);
        this.name = name;
        this.address = address;
        this.overview = overview;
        this.operatingHours = operatingHours;
    }
}
