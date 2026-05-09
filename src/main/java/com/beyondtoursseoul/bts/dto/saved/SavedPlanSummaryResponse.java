package com.beyondtoursseoul.bts.dto.saved;

import com.beyondtoursseoul.bts.domain.saved.UserSavedPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@Schema(description = "저장한 나만의 일정 요약")
public class SavedPlanSummaryResponse {

    private final Long id;
    private final String title;
    private final OffsetDateTime savedAt;

    public static SavedPlanSummaryResponse from(UserSavedPlan plan) {
        return SavedPlanSummaryResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .savedAt(plan.getSavedAt())
                .build();
    }
}
