package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.LockerApiResponseDto;
import com.beyondtoursseoul.bts.service.LockerApiService;
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

    private final LockerApiService lockerApiService;

    @GetMapping("/test")
    public LockerApiResponseDto testLockerApi() {
        // String이 아닌 우리가 만든 DTO 형식으로 JSON을 예쁘게 반환합니다.
        return lockerApiService.fetchLockerData();
    }

    // 추가: DB에 저장하는 동작을 트리거하는 엔드포인트
    @PostMapping("/sync")
    public String syncLockerData() {
        log.info("컨트롤러 진입");
        lockerApiService.syncLockerDataToDb();
        return "데이터 동기화 완료! DB(Supabase)를 확인해보세요.";
    }
}