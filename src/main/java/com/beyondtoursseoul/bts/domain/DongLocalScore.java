package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dong_local_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DongLocalScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dong_code", length = 10)
    private String dongCode;

    private LocalDate date;

    @Column(name = "time_slot", length = 20)
    private String timeSlot;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "breakdown_json", columnDefinition = "text")
    private String breakdownJson;

    @Builder
    public DongLocalScore(String dongCode, LocalDate date, String timeSlot,
                          BigDecimal score, String breakdownJson) {
        this.dongCode = dongCode;
        this.date = date;
        this.timeSlot = timeSlot;
        this.score = score;
        this.breakdownJson = breakdownJson;
    }
}
