package com.beyondtoursseoul.bts.controller.tour;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.TourApiEventItemDto;
import com.beyondtoursseoul.bts.dto.tour.TourApiResponseDto;
import com.beyondtoursseoul.bts.service.tour.TourApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Tour API", description = "관광공사 데이터 연동 및 조회 API")
@RestController
@RequestMapping("/api/v1/tour")
@RequiredArgsConstructor
public class TourController {

    private final TourApiService tourApiService;

    /**
     * 1. API 호출 결과 DTO로 즉시 확인 (DB 저장 X)
     */
    @Operation(summary = "관광공사 API 원본 데이터 확인", description = "DB에 저장하지 않고 API 결과만 DTO로 반환합니다.")
    @GetMapping("/test/fetch")
    public ResponseEntity<TourApiResponseDto<TourApiEventItemDto>> fetchTest(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {

        TourApiResponseDto<TourApiEventItemDto> result = tourApiService.getRawFestivals(lang);
        log.info("문화/행사/축제 데이터 조회 완료: {}건, language: {}", result.getResponse().getBody().getTotalCount(), lang);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. API 호출 후 DB 동기화 실행
     */
    @Operation(summary = "관광공사 데이터 DB 동기화 실행 (임의 실행 X)", description = "API 데이터를 긁어와서 우리 DB에 저장/업데이트합니다.")
    @GetMapping("/test/sync")
    public ResponseEntity<String> syncTest(
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {

        try {
            tourApiService.syncFestivals(lang);
            return ResponseEntity.ok("Successfully synced " + lang + " data to database.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sync failed: " + e.getMessage());
        }
    }
}
