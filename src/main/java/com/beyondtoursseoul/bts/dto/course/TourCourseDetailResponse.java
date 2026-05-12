package com.beyondtoursseoul.bts.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "추천 여행 코스 상세 응답 DTO")
public class TourCourseDetailResponse {
    @Schema(description = "코스 ID")
    private Long id;

    @Schema(description = "코스 제목")
    private String title;

    @Schema(description = "코스 해시태그")
    private String hashtags;

    @Schema(description = "대표 이미지 URL")
    private String featuredImage;

    @Schema(description = "사용자 저장 여부")
    private boolean isSaved;

    @Schema(description = "코스 구성 아이템 목록")
    private List<TourCourseItemResponse> items;

    @Schema(description = "코스 내 관광지 스팟의 찐로컬 지수 평균 (0~1). 없으면 null")
    private BigDecimal avgLocalScore;

    @Schema(description = "avgLocalScore를 0~100%로 반올림. 없으면 null")
    private Integer avgLocalScorePercent;

    @Schema(description = "찐로컬 취향 5단계(0~4). 없으면 null")
    private Integer localBand;
}
