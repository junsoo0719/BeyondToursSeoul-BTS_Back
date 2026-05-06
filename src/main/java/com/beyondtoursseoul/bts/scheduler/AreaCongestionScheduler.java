package com.beyondtoursseoul.bts.scheduler;

import com.beyondtoursseoul.bts.service.AreaCongestionCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AreaCongestionScheduler {

    private final AreaCongestionCollectService areaCongestionCollectService;

    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void collectAreaCongestion() {
        log.info("[AreaCongestionScheduler] collect start");
        areaCongestionCollectService.collectAll();
        log.info("[AreaCongestionScheduler] collect end");
    }
}
