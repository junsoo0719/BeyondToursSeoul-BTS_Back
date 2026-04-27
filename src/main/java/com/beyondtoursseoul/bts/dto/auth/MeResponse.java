package com.beyondtoursseoul.bts.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private String userId;
    private String email;
    private String role;
    private String nickname;
    private String preferredLanguage;
    private Integer visitCount;
    private String localPreference;
}
