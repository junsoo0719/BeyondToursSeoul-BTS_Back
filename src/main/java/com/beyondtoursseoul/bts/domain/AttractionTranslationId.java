package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class AttractionTranslationId implements Serializable {

    @Column(name = "attraction_id")
    private Long attractionId;

    @Column(length = 10)
    private String lang;

    public AttractionTranslationId(Long attractionId, String lang) {
        this.attractionId = attractionId;
        this.lang = lang;
    }
}
