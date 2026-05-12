package com.beyondtoursseoul.bts.domain.course;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tour_course_item_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_item_id", "language"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TourCourseItemTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_item_id", nullable = false)
    private TourCourseItem courseItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TourLanguage language;

    @Column(name = "ai_comment", nullable = false, columnDefinition = "TEXT")
    private String aiComment;
}
