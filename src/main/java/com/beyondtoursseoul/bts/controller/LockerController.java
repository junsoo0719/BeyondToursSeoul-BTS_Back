package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.LockerApiResponseDto;
import com.beyondtoursseoul.bts.service.LockerService;
import com.beyondtoursseoul.bts.service.translation.LockerTranslationService;
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
public class LockerController {

    private final LockerService lockerService;
    private final LockerTranslationService lockerTranslationService;

    /// 물품보관함 데이터 호출 api
    @GetMapping("/test")
    public LockerApiResponseDto testLockerApi() {
        return lockerService.fetchLockerData();
    }

    /// DB에 물품보관함 데이터 insert, update 하는 메서드 - 번역 포함 (2주에 한 번 스케쥴링 설정)
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