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

import java.util.List;

@Slf4j
@Component
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

            List<AreaCongestionRaw> rows =
                    areaCongestionRawRepository.findByAreaCodeOrderByPopulationTimeDesc(SAMPLE_AREA_CODE);

            if (rows.isEmpty()) {
                log.warn("[CityDataSampleRunner] no saved row found. areaCode={}", SAMPLE_AREA_CODE);
                return;
            }

            logSavedRow(rows.get(0));

        } catch (Exception e) {
            log.error("[CityDataSampleRunner] citydata collect sample failed", e);
        }
    }

    private void logSavedRow(AreaCongestionRaw row) {
        log.info("[CityDataSampleRunner] saved row id={}", row.getId());
        log.info("[CityDataSampleRunner] areaCode={}", row.getAreaCode());
        log.info("[CityDataSampleRunner] areaName={}", row.getAreaName());
        log.info("[CityDataSampleRunner] congestionLevel={}", row.getCongestionLevel());
        log.info("[CityDataSampleRunner] congestionMessage={}", row.getCongestionMessage());
        log.info("[CityDataSampleRunner] populationMin={}", row.getPopulationMin());
        log.info("[CityDataSampleRunner] populationMax={}", row.getPopulationMax());
        log.info("[CityDataSampleRunner] populationTime={}", row.getPopulationTime());
        log.info("[CityDataSampleRunner] forecastYn={}", row.getForecastYn());
        log.info("[CityDataSampleRunner] collectedAt={}", row.getCollectedAt());
    }
}
