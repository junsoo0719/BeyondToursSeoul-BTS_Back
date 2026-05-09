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
    private Long contentId; // 관광공사 고유 ID (PK)

    @Column(name = "content_type_id")
    private Long contentTypeId; // 축제/행사 타입 코드 (KOR: 15, ENG: 75 등)

    @Column(name = "first_image")
    private String firstImage; // 대표 이미지 원본 URL

    @Column(name = "first_image2")
    private String firstImage2; // 대표 이미지 썸네일 URL

    @Column(name = "map_x")
    private Double mapX; // 경도 (Longitude)

    @Column(name = "map_y")
    private Double mapY; // 위도 (Latitude)

    @Column(name = "tel")
    private String tel; // 전화번호

    @Column(name = "zipcode")
    private String zipCode; // 우편번호

    @Column(name = "event_start_date")
    private String eventStartDate; // 행사 시작일 (YYYYMMDD)

    @Column(name = "event_end_date")
    private String eventEndDate; // 행사 종료일 (YYYYMMDD)

//    @Column(name = "area_code")
//    private String areaCode; // 지역 코드
//
//    @Column(name = "sigungu_code")
//    private String sigunguCode; // 시군구 코드

    @Column(name = "modified_time")
    private String modifiedTime; // API측 최종 수정 일시 (YYYYMMDDHHMMSS)

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime; // 우리 시스템의 마지막 동기화 일시

    @Column(name = "last_fetch_attempt_time")
    private LocalDateTime lastFetchAttemptTime; // 최근 API 조회(패치) 시도 일시

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TourApiEventTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TourApiEventImage> images = new ArrayList<>();
}
