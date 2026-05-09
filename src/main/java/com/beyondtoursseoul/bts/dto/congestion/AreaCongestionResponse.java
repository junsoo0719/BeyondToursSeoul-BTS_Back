package com.beyondtoursseoul.bts.dto.congestion;

import com.beyondtoursseoul.bts.domain.AreaCongestionRaw;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "지역 혼잡도 조회 응답 DTO")
public class AreaCongestionResponse {

    @Schema(description = "지역 코드", example = "POI009")
    private String areaCode;

    @Schema(description = "지역명", example = "광화문·덕수궁")
    private String areaName;

    @Schema(description = "혼잡도 수준", example = "약간 붐빔")
    private String congestionLevel;

    @Schema(description = "위도", example = "37.5695270714")
    private Double latitude;

    @Schema(description = "경도", example = "126.9768018471")
    private Double longitude;

    @Schema(description = "인구 기준 시각")
    private LocalDateTime populationTime;

    @Schema(description = "수집 시각")
    private OffsetDateTime collectedAt;

    public static AreaCongestionResponse from(AreaCongestionRaw raw) {
        return new AreaCongestionResponse(
                raw.getAreaCode(),
                raw.getAreaName(),
                raw.getCongestionLevel(),
                raw.getLatitude(),
                raw.getLongitude(),
                raw.getPopulationTime(),
                raw.getCollectedAt()
        );
    }
}
