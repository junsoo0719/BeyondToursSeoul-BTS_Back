package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class AttractionLocalScoreId implements Serializable {

    @Column(name = "attraction_id")
    private Long attractionId;

    private LocalDate date;

    private Integer hour;

    public AttractionLocalScoreIrd(Long attractionId, LocalDate date, Integer hour) {
        this.attractionId = attractionId;
        this.date = date;
        this.hour = hour;
    }
}
