package com.beyondtoursseoul.bts.dto.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "관광공사 API 이미지 상세 정보 아이템 DTO")
public class TourApiDetailImageItemDto {
    @JsonAlias("originimgurl")
    @Schema(description = "원본 이미지 URL")
    private String originImgUrl;

    @JsonAlias("smallimageurl")
    @Schema(description = "썸네일 이미지 URL")
    private String smallImgUrl;

    @JsonAlias("cpyrhtDivCd")
    @Schema(description = "저작권 유형")
    private String copyrightType;
}
