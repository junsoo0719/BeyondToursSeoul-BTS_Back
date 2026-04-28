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

    @Column(name = "time_slot", length = 20)
    private String timeSlot;

    public AttractionLocalScoreId(Long attractionId, LocalDate date, String timeSlot) {
        this.attractionId = attractionId;
        this.date = date;
        this.timeSlot = timeSlot;
    }
}
