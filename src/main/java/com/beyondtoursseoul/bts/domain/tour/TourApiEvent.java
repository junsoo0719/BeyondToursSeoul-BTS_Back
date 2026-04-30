package com.beyondtoursseoul.bts.domain.tour;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tour_api_event")
public class TourApiEvent {

    @Id
    @Column(name = "content_id")
    private Long contentId; // 관광공사 고유 ID

    @Column(name = "content_type_id")
    private Long contentTypeId; // 축제/행사 타입 코드

    @Column(name = "first_image")
    private String firstImage;

    @Column(name = "first_image2")
    private String firstImage2;

    @Column(name = "map_x")
    private Double mapX;

    @Column(name = "map_y")
    private Double mapY;

    @Column(name = "tel")
    private String tel;

    @Column(name = "zipcode")
    private String zipCode;

    @Column(name = "event_start_date")
    private String eventStartDate; // YYYYMMDD

    @Column(name = "event_end_date")
    private String eventEndDate; // YYYYMMDD

    @Column(name = "area_code")
    private String areaCode;

    @Column(name = "sigungu_code")
    private String sigunguCode;

    @Column(name = "modified_time")
    private String modifiedTime; // API 제공 수정일시

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime; // 우리 DB 동기화 일시

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TourApiEventTranslation> translations = new ArrayList<>();
}
