package com.beyondtoursseoul.bts.domain.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_saved_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_content_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSavedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_content_id", nullable = false, referencedColumnName = "content_id")
    private TourApiEvent event;

    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    @PrePersist
    public void prePersist() {
        if (savedAt == null) {
            savedAt = OffsetDateTime.now();
        }
    }
}
