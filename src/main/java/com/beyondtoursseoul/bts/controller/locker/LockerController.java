package com.beyondtoursseoul.bts.controller.locker;

import com.beyondtoursseoul.bts.dto.locker.LockerApiResponseDto;
import com.beyondtoursseoul.bts.dto.locker.LockerDetailResponse;
import com.beyondtoursseoul.bts.dto.locker.LockerSummaryResponse;
import com.beyondtoursseoul.bts.service.locker.LockerService;
import com.beyondtoursseoul.bts.service.translation.LockerTranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Locker", description = "서울시 물품보관함 데이터 관리 및 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;
    private final LockerTranslationService lockerTranslationService;

    /**
     * 물품보관함 리스트 조회 (지도용)
     */
    @Operation(summary = "물품보관함 리스트 조회", description = "지도에 표시할 물품보관함 목록을 조회합니다. 언어별 번역을 제공하며, 없으면 한국어로 대체됩니다.")
    @GetMapping
    public ResponseEntity<List<LockerSummaryResponse>> getLockers(
            @Parameter(description = "언어 설정 (KOR, ENG, JPN, CHS, CHT)", example = "KOR")
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {
        return ResponseEntity.ok(lockerService.getLockerList(lang));
    }

    /**
     * 물품보관함 상세 조회
     */
    @Operation(summary = "물품보관함 상세 조회", description = "특정 물품보관함의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<LockerDetailResponse> getLockerDetail(
            @Parameter(description = "물품보관함 시스템 ID (PK)", example = "1")
            @PathVariable Long id,
            @Parameter(description = "언어 설정 (KOR, ENG, JPN, CHS, CHT)", example = "KOR")
            @RequestParam(defaultValue = "KOR") TourLanguage lang) {
        return ResponseEntity.ok(lockerService.getLockerDetail(id, lang));
    }

    /// 물품보관함 데이터 호출 api
    @Operation(
            summary = "물품보관함 외부 API 테스트 호출(3개)",
            description = "서울시 열린데이터 광장 API로부터 실시간 물품보관함 데이터를 가져와 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데이터 호출 성공"),
            @ApiResponse(responseCode = "500", description = "외부 API 통신 오류")})
    @GetMapping("/test")
    public ResponseEntity<LockerApiResponseDto> testLockerApi() {
        return ResponseEntity.ok(lockerService.fetchLockerData());
    }

    /// DB에 물품보관함 데이터 insert, update 하는 메서드 - 번역 포함 (2주에 한 번 스케쥴링 설정)
    @Operation(
            summary = "물품보관함 데이터 DB 동기화 (임의호출X - 토큰 제한)",
            description = "외부 API 데이터를 호출하여 DB(Supabase)에 최신 정보를 Insert 또는 Update합니다. 번역 데이터도 함께 갱신됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동기화 성공"),
            @ApiResponse(responseCode = "500", description = "데이터베이스 작업 실패")})
    @PostMapping("/sync")
    public ResponseEntity<String> syncLockerData() {
        log.info("컨트롤러 진입");
        lockerService.syncLockerDataToDb();
        return ResponseEntity.ok("데이터 동기화 완료! DB(Supabase)를 확인해보세요.");
    }

    /// 번역 메서드 (주석)
//    @PostMapping("translate-test")
//    public String triggerTranslation() {
//        log.info("번역 프로세스 시작");
//        lockerTranslationService.translateAllKoToMultiLang();
//        return "번역 작업 완료. DB를 확인해보세요.";
//    }
}
