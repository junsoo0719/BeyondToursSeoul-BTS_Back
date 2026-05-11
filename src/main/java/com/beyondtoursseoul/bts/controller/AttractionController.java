package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.attraction.AttractionDetailResponse;
import com.beyondtoursseoul.bts.dto.attraction.AttractionSummaryResponse;
import com.beyondtoursseoul.bts.service.AttractionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Attraction", description = "관광지 조회 API")
@RestController
@RequestMapping("/api/v1/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionQueryService attractionQueryService;

    @Operation(
            summary = "관광지 목록 조회",
            description = "찐로컬 지수 score 내림차순으로 관광지 목록을 반환합니다. " +
                    "date 미입력 시 가장 최근 점수 날짜를 자동으로 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<AttractionSummaryResponse>> getList(
            @Parameter(description = "조회 날짜 (yyyy-MM-dd). 미입력 시 DB 최신 날짜 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "시간대. morning/lunch/afternoon/evening/night", example = "afternoon")
            @RequestParam(defaultValue = "afternoon") String timeSlot,
            @Parameter(description = "찐로컬 지수 최솟값 (0~1). 미입력 시 하한 없음")
            @RequestParam(required = false) BigDecimal minScore,
            @Parameter(description = "찐로컬 지수 최댓값 (0~1). 미입력 시 상한 없음")
            @RequestParam(required = false) BigDecimal maxScore,
            @Parameter(description = "언어 코드. ko(기본)/en/zh/ja")
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "ko") String lang) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(attractionQueryService.getList(date, timeSlot, minScore, maxScore, parseLang(lang)));
    }



    @Operation(
            summary = "관광지 목록 페이지네이션 조회",
            description = "카테고리(다국어 가능)와 찐로컬 지수 score 내림차순으로 관광지 목록을 10개씩 페이지 단위로 반환합니다."
    )
    @GetMapping("/page")
    public ResponseEntity<Page<AttractionSummaryResponse>> getListPage(
            @Parameter(description = "카테고리 이름 (예: 음식, Food, Shopping 등)")
            @RequestParam(required = false) String category,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd). 미입력 시 DB 최신 날짜 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "시간대. morning/lunch/afternoon/evening/night", example = "afternoon")
            @RequestParam(defaultValue = "afternoon") String timeSlot,
            @Parameter(description = "찐로컬 지수 최솟값 (0~1). 미입력 시 하한 없음")
            @RequestParam(required = false) BigDecimal minScore,
            @Parameter(description = "찐로컬 지수 최댓값 (0~1). 미입력 시 상한 없음")
            @RequestParam(required = false) BigDecimal maxScore,
            @Parameter(description = "언어 코드. ko(기본)/en/zh/ja")
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "ko") String lang,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지당 데이터 개수", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        // 파라미터에 category를 추가하여 서비스로 넘깁니다.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(attractionQueryService.getListPage(
                        category, date, timeSlot, minScore, maxScore, parseLang(lang), pageable));
    }


    @Operation(
            summary = "관광지 상세 조회",
            description = "관광지 상세 정보를 반환합니다. " +
                    "overview·tel·operatingHours는 첫 요청 시 TourAPI에서 가져와 DB에 저장하며, " +
                    "이후 요청부터는 캐시된 값을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "관광지를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AttractionDetailResponse> getDetail(
            @Parameter(description = "관광지 ID") @PathVariable Long id,
            @Parameter(description = "언어 코드. ko(기본)/en/zh/ja")
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "ko") String lang) {
        return ResponseEntity.ok(attractionQueryService.getDetail(id, parseLang(lang)));
    }

    private String parseLang(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) return "ko";
        return acceptLanguage.split("[,;\\-]")[0].trim().toLowerCase();
    }
}
