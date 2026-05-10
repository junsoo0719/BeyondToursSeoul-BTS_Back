package com.beyondtoursseoul.bts.runner;

import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.DongLocalScoreRepository;
import com.beyondtoursseoul.bts.repository.DongPopulationRawRepository;
import com.beyondtoursseoul.bts.service.AttractionApiService;
import com.beyondtoursseoul.bts.service.AttractionMappingService;
import com.beyondtoursseoul.bts.service.LocalResidentApiService;
import com.beyondtoursseoul.bts.service.score.AttractionScoreService;
import com.beyondtoursseoul.bts.service.score.LocalScoreCalculateService;
import com.beyondtoursseoul.bts.service.score.PopulationCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
//@Component
@RequiredArgsConstructor
@Order(2)
public class InitialDataLoader implements ApplicationRunner {

    private final DongPopulationRawRepository rawRepository;
    private final DongLocalScoreRepository scoreRepository;
    private final AttractionRepository attractionRepository;
    private final AttractionMappingService attractionMappingService;
    private final PopulationCollectService populationCollectService;
    private final LocalScoreCalculateService localScoreCalculateService;
    private final LocalResidentApiService localResidentApiService;
    private final AttractionApiService attractionApiService;
    private final AttractionScoreService attractionScoreService;

    @Override
    public void run(ApplicationArguments args) {
        LocalDate latestDate = localResidentApiService.findLatestAvailableDate();

        if (rawRepository.count() == 0) {
            log.info("초기 4주치 생활인구 데이터 적재 시작");
            for (int i = 27; i >= 0; i--) {
                LocalDate date = latestDate.minusDays(i);
                try {
                    populationCollectService.collect(date);
                } catch (Exception e) {
                    log.warn("날짜 {} 수집 실패 (건너뜀): {}", date, e.getMessage());
                }
            }
            log.info("초기 적재 완료");
        } else {
            log.info("dong_population_raw 데이터가 이미 존재합니다. 수집을 건너뜁니다.");
        }

        if (scoreRepository.count() == 0) {
            log.info("찐로컬 지수 계산 시작: {}", latestDate);
            localScoreCalculateService.calculateAndSave(latestDate);
            log.info("초기 찐로컬 지수 계산 완료");
        } else {
            log.info("dong_local_score 데이터가 이미 존재합니다. 계산을 건너뜁니다.");
        }

        if (attractionRepository.count() == 0) {
            log.info("서울 관광지 초기 수집 시작");
            try {
                attractionRepository.saveAll(attractionApiService.fetchSeoulAttractions());
                log.info("서울 관광지 초기 수집 완료");
            } catch (Exception e) {
                log.warn("서울 관광지 초기 수집 실패 (건너뜀): {}", e.getMessage());
            }
        } else {
            log.info("attraction 데이터가 이미 존재합니다. 수집을 건너뜁니다.");
        }

        if (attractionMappingService.countNullDongCode() > 0) {
            log.info("관광지 행정동 매핑 시작");
            try {
                attractionMappingService.mapDongCodes();
            } catch (Exception e) {
                log.warn("관광지 행정동 매핑 실패 (건너뜀): {}", e.getMessage());
            }
        } else {
            log.info("관광지 행정동 매핑이 이미 완료되어 있습니다.");
        }

        // dong_local_score에 실제로 존재하는 최신 날짜 기준으로 계산 (latestDate와 불일치 방지)
        scoreRepository.findTopByOrderByDateDesc().ifPresentOrElse(
                latestScore -> {
                    log.info("관광지 찐로컬 지수 계산 시작: {}", latestScore.getDate());
                    try {
                        attractionScoreService.calculateAndSave(latestScore.getDate());
                    } catch (Exception e) {
                        log.warn("관광지 찐로컬 지수 계산 실패 (건너뜀): {}", e.getMessage());
                    }
                },
                () -> log.warn("dong_local_score 데이터가 없어 관광지 지수 계산을 건너뜁니다.")
        );
    }
}
