package com.beyondtoursseoul.bts.dto.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "관광공사 API 축제 소개 정보 상세 아이템 DTO")
public class TourApiDetailIntroItemDto {
    @JsonAlias("eventplace")
    @Schema(description = "행사 장소")
    private String eventPlace;

    @JsonAlias("playtime")
    @Schema(description = "공연 시간")
    private String playTime;

    @JsonAlias("usetimefestival")
    @Schema(description = "이용 요금")
    private String useTimeFestival;

    @JsonAlias("program")
    @Schema(description = "행사 프로그램")
    private String program;

    @JsonAlias("agelimit")
    @Schema(description = "관람 가능 연령")
    private String ageLimit;

    @JsonAlias("bookingplace")
    @Schema(description = "예매처")
    private String bookingPlace;

    @JsonAlias("subevent")
    @Schema(description = "부대 행사")
    private String subEvent;

    @JsonAlias("discountinfofestival")
    @Schema(description = "할인 정보")
    private String discountInfoFestival;

    @JsonAlias("spendtimefestival")
    @Schema(description = "관람 소요 시간")
    private String spendTimeFestival;

    @JsonAlias("festivalgrade")
    @Schema(description = "축제 등급")
    private String festivalGrade;

    @JsonAlias("sponsor1")
    @Schema(description = "주최자 정보")
    private String sponsor1;

    @JsonAlias("sponsor1tel")
    @Schema(description = "주최자 연락처")
    private String sponsor1tel;

    @JsonAlias("sponsor2")
    @Schema(description = "주관사 정보")
    private String sponsor2;

    @JsonAlias("sponsor2tel")
    @Schema(description = "주관사 연락처")
    private String sponsor2tel;
}
