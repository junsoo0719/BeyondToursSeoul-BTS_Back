package com.beyondtoursseoul.bts.dto.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "관광공사 API 공통 정보 상세 아이템 DTO")
public class TourApiDetailCommonItemDto {
    @JsonAlias("contentid")
    @Schema(description = "콘텐츠 ID")
    private Long contentId;

    @JsonAlias("overview")
    @Schema(description = "행사 개요 (상세 설명)")
    private String overview;

    @JsonAlias("homepage")
    @Schema(description = "홈페이지 주소 (HTML 포함)")
    private String homepage;

    @JsonAlias("telname")
    @Schema(description = "전화번호 명칭 (담당 부서 등)")
    private String telName;

    @JsonAlias("cpyrhtDivCd")
    @Schema(description = "저작권 유형 (Type1: 출처표시, Type3: 변경금지 등)")
    private String copyrightType;
}
