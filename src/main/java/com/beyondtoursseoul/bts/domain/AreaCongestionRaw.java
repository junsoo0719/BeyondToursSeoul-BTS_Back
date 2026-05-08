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
                        name = "uk_area_congestion_raw_area_code",
                        columnNames = {"area_code"}
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

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "population_time", nullable = false)
    private LocalDateTime populationTime;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Builder
    public AreaCongestionRaw(
            String areaCode,
            String areaName,
            String congestionLevel,
            Double latitude,
            Double longitude,
            LocalDateTime populationTime,
            OffsetDateTime collectedAt
    ) {
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.populationTime = populationTime;
        this.collectedAt = collectedAt;
    }

    public void updateLatest(
            String areaName,
            String congestionLevel,
            Double latitude,
            Double longitude,
            LocalDateTime populationTime,
            OffsetDateTime collectedAt
    ) {
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.populationTime = populationTime;
        this.collectedAt = collectedAt;
    }
}
