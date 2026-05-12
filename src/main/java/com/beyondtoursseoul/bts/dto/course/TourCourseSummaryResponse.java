package com.beyondtoursseoul.bts.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "추천 여행 코스 요약 응답 DTO")
public class TourCourseSummaryResponse {
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

    @Schema(description = "코스 내 관광지 스팟의 찐로컬 지수 평균 (0~1, 최신일·afternoon 기준). 데이터 없으면 null")
    private BigDecimal avgLocalScore;

    @Schema(description = "avgLocalScore를 0~100%로 반올림한 값. 없으면 null")
    private Integer avgLocalScorePercent;

    @Schema(description = "찐로컬 취향 5단계(0=유명 관광 위주 … 4=완전 로컬). avgLocalScore 없으면 null")
    private Integer localBand;
}
