package com.beyondtoursseoul.bts.controller.course;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.course.TourCourseDetailResponse;
import com.beyondtoursseoul.bts.dto.course.TourCourseSummaryResponse;
import com.beyondtoursseoul.bts.dto.saved.ToggleSaveResponse;
import com.beyondtoursseoul.bts.service.course.TourCourseService;
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

@Tag(name = "Tour Course", description = "AI 추천 여행 코스 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class TourCourseController {

    private final TourCourseService tourCourseService;

    @Operation(
            summary = "저장된 여행 코스 목록 조회",
            description = "로그인한 사용자가 저장한 여행 코스 목록을 조회합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @GetMapping("/saved")
    public ResponseEntity<List<TourCourseSummaryResponse>> getSavedCourses(
            @Parameter(description = "언어 설정", example = "KOR")
            @RequestParam(defaultValue = "KOR") TourLanguage lang,
            @AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(tourCourseService.getSavedCourses(lang, userId));
    }

    @Operation(
            summary = "추천 여행 코스 목록 조회",
            description = "전체 추천 여행 코스 목록을 요약 정보 형태로 조회합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @GetMapping
    public ResponseEntity<List<TourCourseSummaryResponse>> getCourseList(
            @Parameter(description = "언어 설정", example = "KOR")
            @RequestParam(defaultValue = "KOR") TourLanguage lang,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(tourCourseService.getCourseList(lang, userId));
    }

    @Operation(
            summary = "추천 여행 코스 상세 조회",
            description = "특정 코스의 상세 정보와 포함된 관광지/행사 목록을 조회합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @GetMapping("/{courseId}")
    public ResponseEntity<TourCourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId,
            @Parameter(description = "언어 설정", example = "KOR")
            @RequestParam(defaultValue = "KOR") TourLanguage lang,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(tourCourseService.getCourseDetail(courseId, lang, userId));
    }

    @Operation(
            summary = "추천 여행 코스 저장/취소 (좋아요)",
            description = "특정 코스를 나의 저장된 목록에 추가하거나 삭제합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @PostMapping("/{courseId}/save")
    public ResponseEntity<?> toggleSaveCourse(
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId,
            @AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        boolean saved = tourCourseService.toggleSaveCourse(courseId, userId);
        return ResponseEntity.ok(new ToggleSaveResponse(saved));
    }
}
