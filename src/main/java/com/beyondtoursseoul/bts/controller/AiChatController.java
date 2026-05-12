package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.AiChatRequest;
import com.beyondtoursseoul.bts.dto.AiChatResponse;
import com.beyondtoursseoul.bts.service.GroqChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final GroqChatService groqChatService;

    @PostMapping("/chat")
    public AiChatResponse chat(
            @RequestBody AiChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return groqChatService.chat(request, resolveProfileUserId(jwt));
    }

    /**
     * 저장 관광지·코스 검증은 profiles.id(UUID) 기준이다.
     * JWT {@code sub}가 UUID가 아니면(다른 IdP/클레임 형식) 파싱 실패로 400이 나지 않도록 null 처리한다.
     */
    private static UUID resolveProfileUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(sub.trim());
        } catch (IllegalArgumentException e) {
            log.warn("[AI chat] JWT subject is not a UUID; treating as anonymous for saved-picks. sub={}", sub);
            return null;
        }
    }
}
