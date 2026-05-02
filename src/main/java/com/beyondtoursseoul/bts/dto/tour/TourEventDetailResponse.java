package com.beyondtoursseoul.bts.dto.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventImage;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "문화행사 상세 조회 응답 DTO")
public class TourEventDetailResponse {
    @Schema(description = "콘텐츠 고유 ID")
    private Long contentId;

    @Schema(description = "행사 제목")
    private String title;

    @Schema(description = "행사 주소")
    private String address;

    @Schema(description = "행사 개요")
    private String overview;

    @Schema(description = "홈페이지 URL (HTML 포함)")
    private String homepage;

    @Schema(description = "대표 전화번호")
    private String tel;

    @Schema(description = "전화번호 명칭 (담당부서 등)")
    private String telName;

    @Schema(description = "행사 장소")
    private String eventPlace;

    @Schema(description = "공연/행사 시간")
    private String playTime;

    @Schema(description = "이용 요금/참가비")
    private String useTimeFestival;

    @Schema(description = "주요 프로그램")
    private String program;

    @Schema(description = "관람 가능 연령")
    private String ageLimit;

    @Schema(description = "예매처")
    private String bookingPlace;

    @Schema(description = "부대 행사")
    private String subEvent;

    @Schema(description = "할인 정보")
    private String discountInfoFestival;

    @Schema(description = "관람 소요 시간")
    private String spendTimeFestival;

    @Schema(description = "축제 등급")
    private String festivalGrade;

    @Schema(description = "주최자 정보")
    private String sponsor1;

    @Schema(description = "주최자 연락처")
    private String sponsor1tel;

    @Schema(description = "주관사 정보")
    private String sponsor2;

    @Schema(description = "주관사 연락처")
    private String sponsor2tel;

    @Schema(description = "행사 시작일 (YYYYMMDD)")
    private String eventStartDate;

    @Schema(description = "행사 종료일 (YYYYMMDD)")
    private String eventEndDate;

    @Schema(description = "GPS 경도 (X)")
    private Double mapX;

    @Schema(description = "GPS 위도 (Y)")
    private Double mapY;

    @Schema(description = "행사 관련 이미지 URL 리스트 (대표 이미지 포함)")
    private List<String> images;

    public TourEventDetailResponse(TourApiEvent event, TourApiEventTranslation translation) {
        this.contentId = event.getContentId();
        this.title = translation.getTitle();
        this.address = translation.getAddress();
        this.overview = translation.getOverview();
        this.homepage = translation.getHomepage();
        this.tel = event.getTel();
        this.telName = translation.getTelName();
        this.eventPlace = translation.getEventPlace();
        this.playTime = translation.getPlayTime();
        this.useTimeFestival = translation.getUseTimeFestival();
        this.program = translation.getProgram();
        this.ageLimit = translation.getAgeLimit();
        this.bookingPlace = translation.getBookingPlace();
        this.subEvent = translation.getSubEvent();
        this.discountInfoFestival = translation.getDiscountInfoFestival();
        this.spendTimeFestival = translation.getSpendTimeFestival();
        this.festivalGrade = translation.getFestivalGrade();
        this.sponsor1 = translation.getSponsor1();
        this.sponsor1tel = translation.getSponsor1tel();
        this.sponsor2 = translation.getSponsor2();
        this.sponsor2tel = translation.getSponsor2tel();
        this.eventStartDate = event.getEventStartDate();
        this.eventEndDate = event.getEventEndDate();
        this.mapX = event.getMapX();
        this.mapY = event.getMapY();
        this.images = event.getImages().stream()
                .map(TourApiEventImage::getOriginImgUrl)
                .collect(Collectors.toList());

        // 대표 이미지가 리스트에 없으면 추가
        if (event.getFirstImage() != null && !this.images.contains(event.getFirstImage())) {
            this.images.add(0, event.getFirstImage());
        }
    }
}
