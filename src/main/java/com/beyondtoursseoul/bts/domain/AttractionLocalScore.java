package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "attraction_local_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttractionLocalScore {

    @EmbeddedId
    private AttractionLocalScoreId id;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Builder
    public AttractionLocalScore(Long attractionId, LocalDate date, String timeSlot, BigDecimal score) {
        this.id = new AttractionLocalScoreId(attractionId, date, timeSlot);
        this.score = score;
    }
}
