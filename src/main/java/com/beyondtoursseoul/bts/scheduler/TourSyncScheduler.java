package com.beyondtoursseoul.bts.scheduler;

import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.service.tour.TourApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourSyncScheduler {

    private final TourApiService tourApiService;

    /**
     * 매일 새벽 5시에 관광공사 축제/행사 데이터를 동기화합니다.
     * 국문 데이터를 먼저 갱신한 후, 다국어 데이터를 순차적으로 업데이트합니다.
     * * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void scheduleTourDataSync() {
        log.info("[Scheduler] 정기 관광공사 데이터 동기화를 시작합니다.");

        try {
            // 1. 국문 데이터 최신화 (기준 데이터)
            log.info("[Scheduler] 국문(KOR) 데이터 동기화 시작...");
            tourApiService.syncFestivals(TourLanguage.KOR);

            // 2. 다국어 데이터 동기화 및 누락분 자동 번역
            TourLanguage[] languages = {
                    TourLanguage.ENG,
                    TourLanguage.JPN,
                    TourLanguage.CHS,
                    TourLanguage.CHT
            };

            for (TourLanguage lang : languages) {
                log.info("[Scheduler] {} 데이터 동기화 및 자동 번역 시작...", lang);
                tourApiService.syncFestivals(lang);
            }

            log.info("[Scheduler] 모든 언어에 대한 데이터 동기화가 완료되었습니다.");
        } catch (Exception e) {
            log.error("[Scheduler] 관광공사 데이터 동기화 중 오류 발생: ", e);
        }
    }
}
