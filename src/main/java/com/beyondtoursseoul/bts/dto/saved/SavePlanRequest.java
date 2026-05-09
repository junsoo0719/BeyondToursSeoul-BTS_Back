package com.beyondtoursseoul.bts.dto.saved;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "나만의 일정 저장 요청 (AI structured JSON 등)")
public class SavePlanRequest {

    @Schema(description = "목록/카드용 제목. 비우면 summary.title 또는 첫 날 라벨로 대체 가능")
    private String title;

    @Schema(description = "일정 본문: { days, summary, budget } 형태 권장")
    private Object structured;
}
