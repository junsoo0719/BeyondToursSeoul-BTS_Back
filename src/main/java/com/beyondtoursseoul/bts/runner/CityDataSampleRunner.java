package com.beyondtoursseoul.bts.runner;

import com.beyondtoursseoul.bts.domain.AreaCongestionRaw;
import com.beyondtoursseoul.bts.repository.AreaCongestionRawRepository;
import com.beyondtoursseoul.bts.service.AreaCongestionCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@RequiredArgsConstructor
@Order(1)
public class CityDataSampleRunner implements ApplicationRunner {

    private static final String SAMPLE_AREA_NAME = "광화문·덕수궁";
    private static final String SAMPLE_AREA_CODE = "POI009";

    private final AreaCongestionCollectService areaCongestionCollectService;
    private final AreaCongestionRawRepository areaCongestionRawRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("citydata-collect-sample")) {
            return;
        }

        log.info("[CityDataSampleRunner] citydata collect sample start. areaName={}", SAMPLE_AREA_NAME);

        try {
            areaCongestionCollectService.collectAll();

            areaCongestionRawRepository.findByAreaCode(SAMPLE_AREA_CODE)
                    .ifPresentOrElse(
                            this::logSavedRow,
                            () -> log.warn("[CityDataSampleRunner] no saved row found. areaCode={}", SAMPLE_AREA_CODE)
                    );
        } catch (Exception e) {
            log.error("[CityDataSampleRunner] citydata collect sample failed", e);
        }
    }

    private void logSavedRow(AreaCongestionRaw row) {
        log.info("[CityDataSampleRunner] saved row id={}", row.getId());
        log.info("[CityDataSampleRunner] areaCode={}", row.getAreaCode());
        log.info("[CityDataSampleRunner] areaName={}", row.getAreaName());
        log.info("[CityDataSampleRunner] congestionLevel={}", row.getCongestionLevel());
        log.info("[CityDataSampleRunner] latitude={}", row.getLatitude());
        log.info("[CityDataSampleRunner] longitude={}", row.getLongitude());
        log.info("[CityDataSampleRunner] populationTime={}", row.getPopulationTime());
        log.info("[CityDataSampleRunner] collectedAt={}", row.getCollectedAt());
    }
}
