package com.beyondtoursseoul.bts.domain.tour;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "tour_api_event_translation",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"content_id", "language"})
    }
)
public class TourApiEventTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private TourApiEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private TourLanguage language;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address")
    private String address;

    @Lob
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;
}
