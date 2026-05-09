package com.beyondtoursseoul.bts.domain.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * AI 챗 등으로 생성한 일정(JSON)을 사용자별로 보관합니다. 공식 {@code tour_courses}와 별개입니다.
 */
@Entity
@Table(name = "user_saved_plans")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSavedPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @Column(length = 500)
    private String title;

    @Column(name = "structured_json", nullable = false, columnDefinition = "text")
    private String structuredJson;

    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    @PrePersist
    public void prePersist() {
        if (savedAt == null) {
            savedAt = OffsetDateTime.now();
        }
    }
}
