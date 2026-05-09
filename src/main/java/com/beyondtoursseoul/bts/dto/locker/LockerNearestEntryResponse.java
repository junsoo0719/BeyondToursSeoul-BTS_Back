package com.beyondtoursseoul.bts.dto.locker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "좌표 기준 가까운 물품보관함과 대략 거리(직선)")
public class LockerNearestEntryResponse {

    private final Long id;

    private final String lockerId;

    private final Double latitude;

    private final Double longitude;

    private final String stationName;

    private final String lockerName;

    private final String detailLocation;

    @Schema(description = "직선 거리(미터), 도보 시간은 참고용")
    private final int distanceMeters;

    public LockerNearestEntryResponse(LockerSummaryResponse summary, double distanceMeters) {
        this.id = summary.getId();
        this.lockerId = summary.getLockerId();
        this.latitude = summary.getLatitude();
        this.longitude = summary.getLongitude();
        this.stationName = summary.getStationName();
        this.lockerName = summary.getLockerName();
        this.detailLocation = summary.getDetailLocation();
        this.distanceMeters = (int) Math.round(distanceMeters);
    }
}
