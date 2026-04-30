package com.beyondtoursseoul.bts.dto.tour;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "관광공사 API 공통 응답 래퍼")
public class TourApiResponseDto<T> {

    private Response<T> response;

    @Data
    public static class Response<T> {
        private Header header;
        private Body<T> body;
    }

    @Data
    public static class Header {
        @Schema(description = "결과 코드 (0000: 성공)", example = "0000")
        private String resultCode;
        @Schema(description = "결과 메시지", example = "OK")
        private String resultMsg;
    }

    @Data
    public static class Body<T> {
        private Items<T> items;
        @Schema(description = "한 페이지 결과 수", example = "10")
        private int numOfRows;
        @Schema(description = "페이지 번호", example = "1")
        private int pageNo;
        @Schema(description = "전체 결과 수", example = "125")
        private int totalCount;
    }


    @Data
    public static class Items<T> {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<T> item;
    }
}
