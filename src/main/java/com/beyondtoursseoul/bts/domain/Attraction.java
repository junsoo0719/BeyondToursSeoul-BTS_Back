package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Entity
@Table(name = "attraction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(length = 200)
    private String name;

    @Column(length = 50)
    private String category;

    private String address;

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point geom;

    @Column(name = "dong_code", length = 10)
    private String dongCode;

    @Column(length = 20)
    private String source;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Builder
    public Attraction(String externalId, String name, String category, String address,
                      Point geom, String dongCode, String source, OffsetDateTime createdAt) {
        this.externalId = externalId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.geom = geom;
        this.dongCode = dongCode;
        this.source = source;
        this.createdAt = createdAt;
    }

    public void updateDongCode(String dongCode) {
        this.dongCode = dongCode;
    }
}
