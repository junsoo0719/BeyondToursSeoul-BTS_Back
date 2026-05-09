package com.beyondtoursseoul.bts.dto.saved;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@Schema(description = "저장한 나만의 일정 상세")
public class SavedPlanDetailResponse {

    private final Long id;
    private final String title;
    private final OffsetDateTime savedAt;
    private final Object structured;
}
