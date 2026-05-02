package com.beyondtoursseoul.bts.dto.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "물품보관함 상세 정보 응답 DTO")
public class LockerDetailResponse {
    @Schema(description = "시스템 고유 ID")
    private Long id;

    @Schema(description = "보관함 고유 ID (서울시)")
    private String lockerId;

    @Schema(description = "위도")
    private Double latitude;

    @Schema(description = "경도")
    private Double longitude;

    @Schema(description = "역명")
    private String stationName;

    @Schema(description = "보관함 이름")
    private String lockerName;

    @Schema(description = "상세 위치")
    private String detailLocation;

    @Schema(description = "전체 보관함 개수")
    private Integer totalCnt;

    @Schema(description = "평일 운영 시간 (시작~종료)")
    private String weekdayOperTime;

    @Schema(description = "주말 운영 시간 (시작~종료)")
    private String weekendOperTime;

    @Schema(description = "기본 요금 정보")
    private String basePriceInfo;

    @Schema(description = "추가 요금 정보")
    private String addPriceInfo;

    @Schema(description = "보관함 크기 정보")
    private String sizeInfo;

    @Schema(description = "보관 제한 물품 정보")
    private String limitItemsInfo;

    public LockerDetailResponse(Locker locker, LockerTranslation translation) {
        this.id = locker.getId();
        this.lockerId = locker.getLckrId();
        this.latitude = locker.getLatitude();
        this.longitude = locker.getLongitude();
        this.stationName = translation.getStationName();
        this.lockerName = translation.getLockerName();
        this.detailLocation = translation.getDetailLocation();
        this.totalCnt = locker.getTotalCnt();
        this.weekdayOperTime = String.format("%s ~ %s", locker.getWeekdayStartTime(), locker.getWeekdayEndTime());
        this.weekendOperTime = String.format("%s ~ %s", locker.getWeekendStartTime(), locker.getWeekendEndTime());
        this.basePriceInfo = translation.getBasePriceInfo();
        this.addPriceInfo = translation.getAddPriceInfo();
        this.sizeInfo = translation.getSizeInfo();
        this.limitItemsInfo = translation.getLimitItemsInfo();
    }
}
