package com.beyondtoursseoul.bts.dto.saved;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@Schema(description = "저장한 행사")
public class SavedEventResponse {

    private final Long contentId;
    private final String title;
    private final String address;
    private final String firstImage;
    private final String eventStartDate;
    private final String eventEndDate;
    private final Double mapX;
    private final Double mapY;
    private final OffsetDateTime savedAt;
}
