package com.beyondtoursseoul.bts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatRequest {
    private String message;
    private String language;
    private List<ChatHistoryMessage> history;

    /**
     * 로컬 여행 선호도 (0 = 관광지 100%, 100 = 로컬 100%).
     * 미전달 시 50(균형)으로 처리.
     */
    private Integer localRatio;

    /**
     * AI 일정에 반영할 저장 관광지 PK 목록(프론트에서 체크한 항목). 인증된 사용자의 저장함에 실제 있는 ID만 서버에서 반영한다.
     */
    private List<Long> savedAttractionIds;

    /**
     * AI 일정에 반영할 저장 공식 코스 PK 목록.
     */
    private List<Long> savedCourseIds;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatHistoryMessage {
        private String role;
        private String content;
    }
}
