package com.beyondtoursseoul.bts.dto.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "물품보관함 요약 정보 응답 DTO (지도/바텀시트용)")
public class LockerSummaryResponse {
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

    public LockerSummaryResponse(Locker locker, LockerTranslation translation) {
        this.id = locker.getId();
        this.lockerId = locker.getLckrId();
        this.latitude = locker.getLatitude();
        this.longitude = locker.getLongitude();
        this.stationName = translation.getStationName();
        this.lockerName = translation.getLockerName();
        this.detailLocation = translation.getDetailLocation();
    }
}
