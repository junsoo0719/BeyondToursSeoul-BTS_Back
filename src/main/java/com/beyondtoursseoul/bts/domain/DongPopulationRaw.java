package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dong_population_raw")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DongPopulationRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dong_code", length = 10, nullable = false)
    private String dongCode;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "time_slot", length = 20, nullable = false)
    private String timeSlot;

    @Column(name = "korean_pop", nullable = false, precision = 10, scale = 4)
    private BigDecimal koreanPop;

    @Column(name = "foreign_pop", nullable = false, precision = 10, scale = 4)
    private BigDecimal foreignPop;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Builder
    public DongPopulationRaw(String dongCode, LocalDate date, String timeSlot,
                              BigDecimal koreanPop, BigDecimal foreignPop) {
        this.dongCode = dongCode;
        this.date = date;
        this.timeSlot = timeSlot;
        this.koreanPop = koreanPop;
        this.foreignPop = foreignPop;
        this.createdAt = OffsetDateTime.now();
    }
}
