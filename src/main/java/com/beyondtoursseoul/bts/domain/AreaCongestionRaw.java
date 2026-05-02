package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "area_congestion_raw",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_area_congestion_raw_area_code_population_time",
                        columnNames = {"area_code", "population_time"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaCongestionRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_code", nullable = false, length = 50)
    private String areaCode;

    @Column(name = "area_name", nullable = false, length = 100)
    private String areaName;

    @Column(name = "congestion_level", nullable = false, length = 50)
    private String congestionLevel;

    @Column(name = "congestion_message", columnDefinition = "text")
    private String congestionMessage;

    @Column(name = "population_min")
    private Integer populationMin;

    @Column(name = "population_max")
    private Integer populationMax;

    @Column(name = "population_time", nullable = false)
    private LocalDateTime populationTime;

    @Column(name = "forecast_yn", length = 1)
    private String forecastYn;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Builder
    public AreaCongestionRaw(
            String areaCode,
            String areaName,
            String congestionLevel,
            String congestionMessage,
            Integer populationMin,
            Integer populationMax,
            LocalDateTime populationTime,
            String forecastYn,
            OffsetDateTime collectedAt,
            String rawPayload
    ) {
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.congestionMessage = congestionMessage;
        this.populationMin = populationMin;
        this.populationMax = populationMax;
        this.populationTime = populationTime;
        this.forecastYn = forecastYn;
        this.collectedAt = collectedAt;
        this.rawPayload = rawPayload;
    }
}
