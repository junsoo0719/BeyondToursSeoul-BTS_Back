package com.beyondtoursseoul.bts.domain.cultural;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cultural_event_translation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CulturalEventTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cultural_event_id")
    private CulturalEvent culturalEvent;

    // 해당 언어 버전의 CID
    @Column(nullable = false, length = 50)
    private String cid;

    // 언어 코드 (ko, en, ja, zh-CN 등)
    @Column(name = "lang_code", nullable = false, length = 10)
    private String langCode;

    // 제목 (post_sj)
    @Column(nullable = false, length = 500)
    private String title;

    // 요약 설명 (sumry)
    @Column(columnDefinition = "TEXT")
    private String summary;

    // 상세 설명 (post_desc) - HTML 포함 가능하므로 LONGTEXT
    @Column(name = "post_desc", columnDefinition = "LONGTEXT")
    private String description;

    // 주소 (new_adres 또는 adres)
    @Column(length = 500)
    private String address;

    // 이용 시간 (cmmn_use_time)
    @Column(name = "use_time", columnDefinition = "TEXT")
    private String useTime;

    // 이용 요금 안내 (trrsrt_use_chrge_guidance)
    @Column(name = "fee_info", columnDefinition = "TEXT")
    private String feeInfo;

    // 휴무일 (closed_days)
    @Column(name = "closed_days")
    private String closedDays;

    // 교통 정보 (subway_info)
    @Column(name = "traffic_info", columnDefinition = "TEXT")
    private String trafficInfo;

    // 태그 (쉼표 구분 문자열)
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Builder
    public CulturalEventTranslation(CulturalEvent culturalEvent, String cid, String langCode, String title,
                                   String summary, String description, String address, String useTime,
                                   String feeInfo, String closedDays, String trafficInfo, String tags) {
        this.culturalEvent = culturalEvent;
        this.cid = cid;
        this.langCode = langCode;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.address = address;
        this.useTime = useTime;
        this.feeInfo = feeInfo;
        this.closedDays = closedDays;
        this.trafficInfo = trafficInfo;
        this.tags = tags;
    }
}
