package com.beyondtoursseoul.bts.runner;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.service.AttractionApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class AttractionDetailBatchRunner implements ApplicationRunner {

    private static final int LOG_INTERVAL = 100;
    private static final long DELAY_MS = 200;

    private final AttractionRepository attractionRepository;
    private final AttractionApiService attractionApiService;

    @Override
    public void run(ApplicationArguments args) {
        List<Attraction> targets = attractionRepository.findByDetailFetchedFalseAndExternalIdNotNull();
        if (targets.isEmpty()) {
            log.info("[DetailBatch] 상세 정보 수집 대상 없음 — 스킵");
            return;
        }

        int total = targets.size();
        int success = 0;
        int failed = 0;
        log.info("[DetailBatch] 상세 정보 일괄 수집 시작: 총 {}건", total);

        for (Attraction attraction : targets) {
            try {
                AttractionApiService.CommonDetail common =
                        attractionApiService.fetchCommonDetail(attraction.getExternalId());
                String operatingHours = attractionApiService.fetchOperatingHours(
                        attraction.getExternalId(), attraction.getCategory());

                attraction.updateDetail(common.overview(), operatingHours, common.tel());
                attractionRepository.save(attraction);
                success++;

                if (success % LOG_INTERVAL == 0) {
                    log.info("[DetailBatch] 진행 중: {} / {} 완료", success, total);
                }

                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[DetailBatch] 인터럽트 — 배치 중단 (완료: {}/{})", success, total);
                return;
            } catch (Exception e) {
                failed++;
                log.warn("[DetailBatch] 실패 (id={}, externalId={}): {}",
                        attraction.getId(), attraction.getExternalId(), e.getMessage());
            }
        }

        log.info("[DetailBatch] 완료 — 성공: {}건, 실패: {}건 / 전체: {}건", success, failed, total);
    }
}
