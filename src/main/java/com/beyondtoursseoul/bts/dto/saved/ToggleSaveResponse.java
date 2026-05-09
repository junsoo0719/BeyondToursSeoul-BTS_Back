package com.beyondtoursseoul.bts.dto.saved;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "저장 토글 결과")
public class ToggleSaveResponse {

    @Schema(description = "true면 저장됨, false면 저장 해제됨")
    private final boolean saved;
}
