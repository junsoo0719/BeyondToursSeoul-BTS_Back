package com.beyondtoursseoul.bts.dto.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "관광공사 API 행사/축제 아이템 정보")
public class TourApiEventItemDto {

    @JsonAlias("contentid")
    @Schema(description = "콘텐츠 고유 ID", example = "3310483")
    private Long contentId;

    @JsonAlias("contenttypeid")
    @Schema(description = "콘텐츠 타입 ID (축제/행사: 15)", example = "15")
    private Long contentTypeId;

    @JsonAlias("title")
    @Schema(description = "행사 명칭", example = "어린이·가족 예술축제")
    private String title;

    @JsonAlias("addr1")
    @Schema(description = "주소", example = "서울특별시 양천구 남부순환로64길 2")
    private String addr1;

    @JsonAlias("addr2")
    @Schema(description = "상세 주소 (장소명 등)", example = "서울문화예술교육센터 양천")
    private String addr2;

    @JsonAlias("zipcode")
    @Schema(description = "우편번호", example = "07916")
    private String zipCode;

    @JsonAlias("firstimage")
    @Schema(description = "대표 이미지 URL (원본)", example = "https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg")
    private String firstImage;

    @JsonAlias("firstimage2")
    @Schema(description = "대표 이미지 URL (썸네일)", example = "https://tong.visitkorea.or.kr/cms/resource/18/4012118_image3_1.jpg")
    private String firstImage2;

    @JsonAlias("mapx")
    @Schema(description = "GPS 경도 (Longitude)", example = "126.83214572562274")
    private Double mapX;

    @JsonAlias("mapy")
    @Schema(description = "GPS 위도 (Latitude)", example = "37.52822008764788")
    private Double mapY;

    @JsonAlias("tel")
    @Schema(description = "전화번호", example = "02-2697-0014")
    private String tel;

    @JsonAlias("eventstartdate")
    @Schema(description = "행사 시작일 (YYYYMMDD)", example = "20260502")
    private String eventStartDate;

    @JsonAlias("eventenddate")
    @Schema(description = "행사 종료일 (YYYYMMDD)", example = "20260503")
    private String eventEndDate;

    @JsonAlias("areacode")
    @Schema(description = "지역 코드", example = "1")
    private String areaCode;

    @JsonAlias("sigungucode")
    @Schema(description = "시군구 코드", example = "1")
    private String sigunguCode;

    @JsonAlias("lDongRegnCd")
    @Schema(description = "법정동 지역코드 (서울: 11)", example = "11")
    private String localRegionCode;

    @JsonAlias("lDongSignguCd")
    @Schema(description = "법정동 시군구코드", example = "470")
    private String localSigunguCode;

    @JsonAlias("modifiedtime")
    @Schema(description = "최종 수정 시간 (YYYYMMDDHHMMSS)", example = "20260414180928")
    private String modifiedTime;
}
