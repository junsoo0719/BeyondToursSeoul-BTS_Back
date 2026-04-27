package com.beyondtoursseoul.bts.scheduler;

import com.beyondtoursseoul.bts.service.locker.LockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockerSyncScheduler {

    private final LockerService lockerService;

    /**
     * 매일 새벽 4시에 물품보관함 데이터를 동기화
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 4 1,15 * *")
    public void scheduleLockerDataSync() {
        log.info("정기 물품보관함 데이터 업데이트를 시작합니다.");
        try {
            lockerService.syncLockerDataToDb();
            log.info("정기 물품보관함 데이터 업데이트가 완료되었습니다.");
        } catch (Exception e) {
            log.error("물품보관함 업데이트 중 오류가 발생했습니다: ", e);
        }
    }
}
