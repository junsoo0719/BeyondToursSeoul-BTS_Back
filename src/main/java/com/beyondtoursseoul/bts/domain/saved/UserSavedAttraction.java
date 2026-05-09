package com.beyondtoursseoul.bts.domain.saved;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.Profile;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_saved_attractions",
        uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "attraction_id" })
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSavedAttraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    @PrePersist
    public void prePersist() {
        if (savedAt == null) {
            savedAt = OffsetDateTime.now();
        }
    }
}
