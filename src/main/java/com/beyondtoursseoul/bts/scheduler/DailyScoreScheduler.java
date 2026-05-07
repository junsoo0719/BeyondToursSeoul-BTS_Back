package com.beyondtoursseoul.bts.scheduler;

import com.beyondtoursseoul.bts.repository.DongLocalScoreRepository;
import com.beyondtoursseoul.bts.service.LocalResidentApiService;
import com.beyondtoursseoul.bts.service.score.AttractionScoreService;
import com.beyondtoursseoul.bts.service.score.LocalScoreCalculateService;
import com.beyondtoursseoul.bts.service.score.PopulationCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyScoreScheduler {

    private final LocalResidentApiService localResidentApiService;
    private final PopulationCollectService populationCollectService;
    private final LocalScoreCalculateService localScoreCalculateService;
    private final AttractionScoreService attractionScoreService;
    private final DongLocalScoreRepository localScoreRepository;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void run() {
        log.info("[DailyScoreScheduler] 시작");
        try {
            LocalDate targetDate = localResidentApiService.findLatestAvailableDate();

            if (localScoreRepository.existsByDate(targetDate)) {
                log.info("[DailyScoreScheduler] {} 데이터 이미 존재 — 스킵", targetDate);
                return;
            }

            populationCollectService.collect(targetDate);
            localScoreCalculateService.calculateAndSave(targetDate);
            attractionScoreService.calculateAndSave(targetDate);
            log.info("[DailyScoreScheduler] 완료: {}", targetDate);
        } catch (Exception e) {
            log.error("[DailyScoreScheduler] 실패 — {}", e.getMessage(), e);
        }
    }
}
