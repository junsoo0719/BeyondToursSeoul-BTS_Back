package com.beyondtoursseoul.bts.domain.cultural;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cultural_event_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CulturalEventImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultural_event_id")
    private CulturalEvent culturalEvent;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Builder
    public CulturalEventImage(CulturalEvent culturalEvent, String imageUrl) {
        this.culturalEvent = culturalEvent;
        this.imageUrl = imageUrl;
    }
}
