package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.saved.*;
import com.beyondtoursseoul.bts.service.saved.UserSavedAttractionService;
import com.beyondtoursseoul.bts.service.saved.UserSavedEventService;
import com.beyondtoursseoul.bts.service.saved.UserSavedPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "My saves", description = "계정별 저장함 — 관광지·행사·나만의 일정(AI structured)")
@RestController
@RequestMapping("/api/v1/me/saved")
@RequiredArgsConstructor
public class MeSavedController {

    private final UserSavedAttractionService userSavedAttractionService;
    private final UserSavedEventService userSavedEventService;
    private final UserSavedPlanService userSavedPlanService;

    @Operation(summary = "저장한 관광지 목록", security = @SecurityRequirement(name = "jwtAuth"))
    @GetMapping("/attractions")
    public ResponseEntity<List<SavedAttractionResponse>> savedAttractions(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userSavedAttractionService.listSaved(userId));
    }

    @Operation(summary = "관광지 저장/취소 (토글)", security = @SecurityRequirement(name = "jwtAuth"))
    @PostMapping("/attractions/{attractionId}")
    public ResponseEntity<ToggleSaveResponse> toggleAttraction(
            @Parameter(description = "관광지 PK (attraction.id)")
            @PathVariable Long attractionId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean saved = userSavedAttractionService.toggleSave(attractionId, userId);
        return ResponseEntity.ok(new ToggleSaveResponse(saved));
    }

    @Operation(summary = "저장한 행사 목록", security = @SecurityRequirement(name = "jwtAuth"))
    @GetMapping("/events")
    public ResponseEntity<List<SavedEventResponse>> savedEvents(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userSavedEventService.listSaved(userId));
    }

    @Operation(summary = "행사 저장/취소 (토글)", security = @SecurityRequirement(name = "jwtAuth"))
    @PostMapping("/events/{contentId}")
    public ResponseEntity<ToggleSaveResponse> toggleEvent(
            @Parameter(description = "행사 PK (tour_api_event.content_id)")
            @PathVariable Long contentId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean saved = userSavedEventService.toggleSave(contentId, userId);
        return ResponseEntity.ok(new ToggleSaveResponse(saved));
    }

    @Operation(summary = "저장한 나만의 일정 목록", security = @SecurityRequirement(name = "jwtAuth"))
    @GetMapping("/plans")
    public ResponseEntity<List<SavedPlanSummaryResponse>> savedPlans(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userSavedPlanService.listPlans(userId));
    }

    @Operation(summary = "나만의 일정 저장", security = @SecurityRequirement(name = "jwtAuth"))
    @PostMapping("/plans")
    public ResponseEntity<SavedPlanSummaryResponse> savePlan(
            @RequestBody SavePlanRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userSavedPlanService.savePlan(body, userId));
    }

    @Operation(summary = "저장한 일정 상세 (structured JSON)", security = @SecurityRequirement(name = "jwtAuth"))
    @GetMapping("/plans/{planId}")
    public ResponseEntity<SavedPlanDetailResponse> getPlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userSavedPlanService.getPlan(planId, userId));
    }

    @Operation(summary = "저장한 일정 삭제", security = @SecurityRequirement(name = "jwtAuth"))
    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        userSavedPlanService.deletePlan(planId, userId);
        return ResponseEntity.noContent().build();
    }
}
