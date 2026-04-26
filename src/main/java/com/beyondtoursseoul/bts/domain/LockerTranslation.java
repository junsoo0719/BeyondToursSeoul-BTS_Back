package com.beyondtoursseoul.bts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "locker_translations",
        // 같은 보관함에 같은 언어가 2번 들어가지 않도록 복합 유니크 키 설정
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_locker_language",
                        columnNames = {"locker_id", "language_code"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LockerTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id", nullable = false)
    private Locker locker;

    // 언어 코드 (ko, en, ja, zh)
    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    // 요약/상세 공통: 역명 (stnNm)
    @Column(name = "station_name", length = 100)
    private String stationName;

    // 요약/상세 공통: 보관함 이름 (lckrNm)
    @Column(name = "locker_name", length = 100)
    private String lockerName;

    // 요약: 상세 위치/가는 길 (lckrDtlLocNm)
    @Column(name = "detail_location", length = 255)
    private String detailLocation;

    // 상세: 기본 요금 설명 (utztnCrgExpln) - 텍스트가 매우 길 수 있으므로 TEXT 타입으로 변경
    @Column(name = "base_price_info", columnDefinition = "TEXT")
    private String basePriceInfo;

    // 상세: 추가 요금 설명 (addCrgExpln) - 텍스트가 매우 길 수 있으므로 TEXT 타입으로 변경
    @Column(name = "add_price_info", columnDefinition = "TEXT")
    private String addPriceInfo;

    // 상세: 크기 설명 (lckrWdthLenExpln, lckrDpthExpln, lckrHgtExpln 등 조합)
    @Column(name = "size_info", length = 255)
    private String sizeInfo;

    // 상세: 보관 제한 물품 (kpngLmtLckrExpln)
    @Column(name = "limit_items_info", columnDefinition = "TEXT")
    private String limitItemsInfo;

    // 연관관계 편의 메서드
    public void setLocker(Locker locker) {
        this.locker = locker;
        if (!locker.getTranslations().contains(this)) {
            locker.getTranslations().add(this);
        }
    }

    @Builder
    public LockerTranslation(Locker locker, String languageCode, String stationName,
                             String lockerName, String detailLocation, String basePriceInfo,
                             String addPriceInfo, String sizeInfo, String limitItemsInfo) {
        this.languageCode = languageCode;
        this.stationName = stationName;
        this.lockerName = lockerName;
        this.detailLocation = detailLocation;
        this.basePriceInfo = basePriceInfo;
        this.addPriceInfo = addPriceInfo;
        this.sizeInfo = sizeInfo;
        this.limitItemsInfo = limitItemsInfo;
        
        if (locker != null) {
            setLocker(locker);
        }
    }

    public void update(String stationName, String lockerName, String detailLocation,
                       String basePriceInfo, String addPriceInfo, String sizeInfo, String limitItemsInfo) {
        this.stationName = stationName;
        this.lockerName = lockerName;
        this.detailLocation = detailLocation;
        this.basePriceInfo = basePriceInfo;
        this.addPriceInfo = addPriceInfo;
        this.sizeInfo = sizeInfo;
        this.limitItemsInfo = limitItemsInfo;
    }
}
