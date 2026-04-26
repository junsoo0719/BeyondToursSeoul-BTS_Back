package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Table(name = "dong_boundary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DongBoundary {

    @Id
    @Column(name = "dong_code", length = 10)
    private String dongCode;

    @Column(name = "dong_name", length = 50)
    private String dongName;

    @Column(columnDefinition = "GEOMETRY(MultiPolygon, 4326)")
    private MultiPolygon geom;

    @Builder
    public DongBoundary(String dongCode, String dongName, MultiPolygon geom) {
        this.dongCode = dongCode;
        this.dongName = dongName;
        this.geom = geom;
    }
}
