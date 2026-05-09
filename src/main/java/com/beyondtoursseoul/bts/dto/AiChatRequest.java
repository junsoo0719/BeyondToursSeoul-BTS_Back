package com.beyondtoursseoul.bts.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiChatRequest {
    private String message;
    private String language;
    private List<ChatHistoryMessage> history;

    /**
     * 로컬 여행 선호도 (0 = 관광지 100%, 100 = 로컬 100%).
     * 미전달 시 50(균형)으로 처리.
     */
    private Integer localRatio;

    @Getter
    @NoArgsConstructor
    public static class ChatHistoryMessage {
        private String role;
        private String content;
    }
}
