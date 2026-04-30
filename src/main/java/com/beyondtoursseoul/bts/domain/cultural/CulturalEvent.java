package com.beyondtoursseoul.bts.domain.cultural;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cultural_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CulturalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비짓서울 API의 기준 CID (중복 방지 및 업데이트 기준)
    @Column(name = "master_cid", unique = true, nullable = false, length = 50)
    private String masterCid;

    // 카테고리 코드 (예: Cg1x6l1)
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    // 카테고리 경로 (예: 문화관광 > 전시시설)
    @Column(name = "category_depth")
    private String categoryDepth;

    // 대표 이미지 URL
    @Column(name = "main_img_url", length = 500)
    private String mainImgUrl;

    // 행사 시작일
    @Column(name = "start_date")
    private LocalDate startDate;

    // 행사 종료일
    @Column(name = "end_date")
    private LocalDate endDate;

    // 위도 (map_position_y)
    @Column(nullable = false)
    private Double latitude;

    // 경도 (map_position_x)
    @Column(nullable = false)
    private Double longitude;

    // 전화번호
    @Column(name = "tel_no", length = 50)
    private String telNo;

    // 홈페이지 URL
    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    // 무료 여부 (trrsrt_use_chrge가 'F'면 true, 'C'면 false)
    @Column(name = "is_free")
    private Boolean isFree;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "culturalEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CulturalEventTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "culturalEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CulturalEventImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Builder
    public CulturalEvent(String masterCid, String categoryCode, String categoryDepth, String mainImgUrl,
                        LocalDate startDate, LocalDate endDate, Double latitude, Double longitude,
                        String telNo, String homepageUrl, Boolean isFree) {
        this.masterCid = masterCid;
        this.categoryCode = categoryCode;
        this.categoryDepth = categoryDepth;
        this.mainImgUrl = mainImgUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.telNo = telNo;
        this.homepageUrl = homepageUrl;
        this.isFree = isFree;
    }

    public void update(String categoryCode, String categoryDepth, String mainImgUrl,
                       LocalDate startDate, LocalDate endDate, Double latitude, Double longitude,
                       String telNo, String homepageUrl, Boolean isFree) {
        this.categoryCode = categoryCode;
        this.categoryDepth = categoryDepth;
        this.mainImgUrl = mainImgUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.telNo = telNo;
        this.homepageUrl = homepageUrl;
        this.isFree = isFree;
    }
}
