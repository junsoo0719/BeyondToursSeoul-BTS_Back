package com.beyondtoursseoul.bts.dto.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "문화행사 리스트 조회 응답 DTO")
public class TourEventSummaryResponse {
    @Schema(description = "콘텐츠 고유 ID")
    private Long contentId;

    @Schema(description = "행사 제목")
    private String title;

    @Schema(description = "행사 주소")
    private String address;

    @Schema(description = "대표 이미지 URL")
    private String firstImage;

    @Schema(description = "행사 시작일 (YYYYMMDD)")
    private String eventStartDate;

    @Schema(description = "행사 종료일 (YYYYMMDD)")
    private String eventEndDate;

    @Schema(description = "GPS 경도 (X)")
    private Double mapX;

    @Schema(description = "GPS 위도 (Y)")
    private Double mapY;

    public TourEventSummaryResponse(TourApiEvent event, TourApiEventTranslation translation) {
        this.contentId = event.getContentId();
        this.title = translation.getTitle();
        this.address = translation.getAddress();
        this.firstImage = event.getFirstImage();
        this.eventStartDate = event.getEventStartDate();
        this.eventEndDate = event.getEventEndDate();
        this.mapX = event.getMapX();
        this.mapY = event.getMapY();
    }
}
