package com.beyondtoursseoul.bts.controller.locker;

import com.beyondtoursseoul.bts.dto.locker.LockerApiResponseDto;
import com.beyondtoursseoul.bts.service.locker.LockerService;
import com.beyondtoursseoul.bts.service.translation.LockerTranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/lockers")
@RequiredArgsConstructor
@Tag(name = "Locker", description = "서울시 물품보관함 데이터 관리 및 동기화 API")
public class LockerController {

    private final LockerService lockerService;
    private final LockerTranslationService lockerTranslationService;


    /// 물품보관함 데이터 호출 api
    @Operation(
            summary = "물품보관함 외부 API 테스트 호출(3개)",
            description = "서울시 열린데이터 광장 API로부터 실시간 물품보관함 데이터를 가져와 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데이터 호출 성공"),
            @ApiResponse(responseCode = "500", description = "외부 API 통신 오류")})
    @GetMapping("/test")
    public LockerApiResponseDto testLockerApi() {
        return lockerService.fetchLockerData();
    }

    /// DB에 물품보관함 데이터 insert, update 하는 메서드 - 번역 포함 (2주에 한 번 스케쥴링 설정)
    @Operation(
            summary = "물품보관함 데이터 DB 동기화 (임의호출X - 토큰 제한)",
            description = "외부 API 데이터를 호출하여 DB(Supabase)에 최신 정보를 Insert 또는 Update합니다. 번역 데이터도 함께 갱신됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동기화 성공"),
            @ApiResponse(responseCode = "500", description = "데이터베이스 작업 실패")})
    @PostMapping("/sync")
    public String syncLockerData() {
        log.info("컨트롤러 진입");
        lockerService.syncLockerDataToDb();
        return "데이터 동기화 완료! DB(Supabase)를 확인해보세요.";
    }

    /// 번역 메서드 (주석)
//    @PostMapping("translate-test")
//    public String triggerTranslation() {
//        log.info("번역 프로세스 시작");
//        lockerTranslationService.translateAllKoToMultiLang();
//        return "번역 작업 완료. DB를 확인해보세요.";
//    }
}
