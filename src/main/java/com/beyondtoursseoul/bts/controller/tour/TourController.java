package com.beyondtoursseoul.bts.controller.tour;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.TourApiEventItemDto;
import com.beyondtoursseoul.bts.dto.tour.TourApiResponseDto;
import com.beyondtoursseoul.bts.dto.tour.TourEventSummaryResponse;
import com.beyondtoursseoul.bts.service.tour.TourApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tour API", description = "관광공사 데이터 연동 및 조회 API")
@RestController
@RequestMapping("/api/v1/tour")
@RequiredArgsConstructor
public class TourController {

    private final TourApiService tourApiService;
    private final com.beyondtoursseoul.bts.service.tour.TourQueryService tourQueryService;

    /**
     * 문화행사 리스트 조회
     */
    @Operation(summary = "문화행사 리스트 조회", description = "특정 언어에 맞는 문화행사 목록을 조회합니다. 해당 언어 데이터가 없으면 국문(KOR) 데이터를 반환합니다.")
    @GetMapping("/events")
    public ResponseEntity<List<TourEventSummaryResponse>> getEvents(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {
        return ResponseEntity.ok(tourQueryService.getEventList(lang));
    }

    /**
     * 문화행사 상세 조회
     */
    @Operation(summary = "문화행사 상세 조회", description = "특정 문화행사의 상세 정보를 조회합니다.")
    @GetMapping("/events/{contentId}")
    public ResponseEntity<com.beyondtoursseoul.bts.dto.tour.TourEventDetailResponse> getEventDetail(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {
        return ResponseEntity.ok(tourQueryService.getEventDetail(contentId, lang));
    }

    /**
     * 1. API 호출 결과 DTO로 즉시 확인 (DB 저장 X)
     */
    @Operation(summary = "관광공사 API 원본 데이터 확인", description = "DB에 저장하지 않고 API 결과만 DTO로 반환합니다.")
    @GetMapping("/test/fetch")
    public ResponseEntity<TourApiResponseDto<TourApiEventItemDto>> fetchTest(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {

        TourApiResponseDto<TourApiEventItemDto> result = tourApiService.getRawFestivals(lang);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. API 호출 후 DB 동기화 실행 (전체)
     */
    @Operation(summary = "관광공사 데이터 DB 전체 동기화 실행", description = "API 데이터를 긁어와서 우리 DB에 전체 저장/업데이트합니다.")
    @GetMapping("/test/sync-all")
    public ResponseEntity<String> syncAllTest(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {

        try {
            tourApiService.syncFestivals(lang);
            return ResponseEntity.ok("Successfully synced " + lang + " all data to database.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sync failed: " + e.getMessage());
        }
    }

    /**
     * 3. 딱 한 건의 데이터만 DB 동기화 실행 (테스트용)
     */
    @Operation(summary = "관광공사 데이터 DB 단건 동기화 실행", description = "API 데이터 중 첫 번째 한 건만 상세 정보(개요, 장소, 요금, 이미지 등)를 포함하여 저장합니다.")
    @GetMapping("/test/sync-one")
    public ResponseEntity<String> syncOneTest(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {

        try {
            tourApiService.syncOneFestival(lang);
            return ResponseEntity.ok("Successfully synced " + lang + " ONE data with details to database.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sync failed: " + e.getMessage());
        }
    }
}
