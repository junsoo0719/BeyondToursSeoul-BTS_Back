package com.beyondtoursseoul.bts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class AiChatResponse {
    private String answer;
    private Map<String, Object> structured;
    private String model;
}
