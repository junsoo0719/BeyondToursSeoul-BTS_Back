package com.beyondtoursseoul.bts.service.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedPlan;
import com.beyondtoursseoul.bts.dto.saved.SavePlanRequest;
import com.beyondtoursseoul.bts.dto.saved.SavedPlanDetailResponse;
import com.beyondtoursseoul.bts.dto.saved.SavedPlanSummaryResponse;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.saved.UserSavedPlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSavedPlanService {

    private final UserSavedPlanRepository userSavedPlanRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SavedPlanSummaryResponse> listPlans(UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return userSavedPlanRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(SavedPlanSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavedPlanDetailResponse getPlan(Long planId, UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        UserSavedPlan plan = userSavedPlanRepository.findByIdAndUser(planId, user)
                .orElseThrow(() -> new IllegalArgumentException("저장된 일정을 찾을 수 없습니다."));
        JsonNode structured;
        try {
            structured = objectMapper.readTree(plan.getStructuredJson());
        } catch (Exception e) {
            structured = objectMapper.createObjectNode();
        }
        return SavedPlanDetailResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .savedAt(plan.getSavedAt())
                .structured(structured)
                .build();
    }

    @Transactional
    public SavedPlanSummaryResponse savePlan(SavePlanRequest request, UUID userId) {
        if (request.getStructured() == null || request.getStructured().isNull()) {
            throw new IllegalArgumentException("structured 본문이 필요합니다.");
        }
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String json;
        try {
            json = objectMapper.writeValueAsString(request.getStructured());
        } catch (Exception e) {
            throw new IllegalArgumentException("structured JSON 처리에 실패했습니다.");
        }
        String title = resolveTitle(request.getTitle(), request.getStructured());
        UserSavedPlan plan = UserSavedPlan.builder()
                .user(user)
                .title(title)
                .structuredJson(json)
                .build();
        userSavedPlanRepository.save(plan);
        return SavedPlanSummaryResponse.from(plan);
    }

    @Transactional
    public void deletePlan(Long planId, UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        UserSavedPlan plan = userSavedPlanRepository.findByIdAndUser(planId, user)
                .orElseThrow(() -> new IllegalArgumentException("저장된 일정을 찾을 수 없습니다."));
        userSavedPlanRepository.delete(plan);
    }

    private String resolveTitle(String requestTitle, JsonNode structured) {
        if (requestTitle != null && !requestTitle.isBlank()) {
            return requestTitle.trim();
        }
        if (structured.has("summary") && structured.get("summary").isObject()) {
            JsonNode summary = structured.get("summary");
            if (summary.has("title") && !summary.get("title").asText("").isBlank()) {
                return summary.get("title").asText().trim();
            }
        }
        if (structured.has("days") && structured.get("days").isArray() && structured.get("days").size() > 0) {
            JsonNode firstDay = structured.get("days").get(0);
            if (firstDay.has("label") && !firstDay.get("label").asText("").isBlank()) {
                return firstDay.get("label").asText().trim();
            }
        }
        return "나만의 일정";
    }
}
