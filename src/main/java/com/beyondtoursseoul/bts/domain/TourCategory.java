package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tour_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourCategory {

    @Id
    @Column(length = 10)
    private String code;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Builder
    public TourCategory(String code, String name, Integer level) {
        this.code = code;
        this.name = name;
        this.level = level;
    }
}
