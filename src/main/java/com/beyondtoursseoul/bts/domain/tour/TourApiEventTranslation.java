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
    private String title; // 행사 제목

    @Column(name = "address")
    private String address; // 주소

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview; // [상세-공통] 행사 개요

    @Column(name = "homepage", length = 1000)
    private String homepage; // [상세-공통] 홈페이지 URL (HTML 포함)

    @Column(name = "tel_name")
    private String telName; // [상세-공통] 전화번호 명칭

    @Column(name = "event_place")
    private String eventPlace; // [상세-소개] 행사 장소 (예: 올림픽공원)

    @Column(name = "play_time")
    private String playTime; // [상세-소개] 공연/행사 시간 (예: 10:00~18:00)

    @Column(name = "use_time_festival", length = 1000)
    private String useTimeFestival; // [상세-소개] 이용 요금/참가비 정보

    @Column(name = "program", columnDefinition = "TEXT")
    private String program; // [상세-소개] 주요 프로그램 내용

    @Column(name = "age_limit")
    private String ageLimit; // [상세-소개] 관람 가능 연령

    @Column(name = "booking_place")
    private String bookingPlace; // [상세-소개] 예매처

    @Column(name = "sub_event", length = 1000)
    private String subEvent; // [상세-소개] 부대 행사

    @Column(name = "discount_info_festival", length = 1000)
    private String discountInfoFestival; // [상세-소개] 할인 정보

    @Column(name = "spend_time_festival")
    private String spendTimeFestival; // [상세-소개] 관람 소요 시간

    @Column(name = "festival_grade")
    private String festivalGrade; // [상세-소개] 축제 등급

    @Column(name = "sponsor1")
    private String sponsor1; // [상세-소개] 주최자 정보

    @Column(name = "sponsor1tel")
    private String sponsor1tel; // [상세-소개] 주최자 전화번호

    @Column(name = "sponsor2")
    private String sponsor2; // [상세-소개] 주관사 정보

    @Column(name = "sponsor2tel")
    private String sponsor2tel; // [상세-소개] 주관사 전화번호

    @Column(name = "is_auto_translated")
    @Builder.Default
    private Boolean isAutoTranslated = false; // [추가] 구글 번역기를 통한 자동 번역 여부

    @Column(name = "last_translated_modified_time")
    private String lastTranslatedModifiedTime; // [추가] 번역 당시의 원본 수정 일시
}
