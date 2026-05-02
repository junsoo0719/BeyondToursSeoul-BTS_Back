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

    @Getter
    @NoArgsConstructor
    public static class ChatHistoryMessage {
        private String role;
        private String content;
    }
}
