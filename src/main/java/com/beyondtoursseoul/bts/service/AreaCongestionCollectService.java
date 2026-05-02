package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.AreaCongestionRaw;
import com.beyondtoursseoul.bts.dto.CityDataApiResponseDto;
import com.beyondtoursseoul.bts.repository.AreaCongestionRawRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaCongestionCollectService {

    private static final DateTimeFormatter POPULATION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SeoulCityDataApiService seoulCityDataApiService;
    private final AreaCongestionRawRepository areaCongestionRawRepository;

    public enum CollectResult {
        SUCCESS,
        SKIPPED,
        FAILED
    }

    @Transactional
    public CollectResult collectOne(String areaName) {
        log.info("[AreaCongestionCollectService] collect start. areaName={}", areaName);

        CityDataApiResponseDto response = seoulCityDataApiService.fetchByArea(areaName);

        if (response == null || response.getCityData() == null) {
            log.warn("[AreaCongestionCollectService] cityData is null. areaName={}", areaName);
            return CollectResult.FAILED;
        }

        if (response.getCityData().getLivePopulationStatuses() == null
                || response.getCityData().getLivePopulationStatuses().isEmpty()) {
            log.warn("[AreaCongestionCollectService] livePopulationStatuses is empty. areaName={}", areaName);
            return CollectResult.FAILED;
        }

        CityDataApiResponseDto.CityData cityData = response.getCityData();
        CityDataApiResponseDto.LivePopulationStatus status = cityData.getLivePopulationStatuses().get(0);

        LocalDateTime populationTime = parsePopulationTime(status.getPopulationTime());
        Integer populationMin = parseInteger(status.getAreaPpltnMin());
        Integer populationMax = parseInteger(status.getAreaPpltnMax());

        boolean exists = areaCongestionRawRepository.existsByAreaCodeAndPopulationTime(
                cityData.getAreaCode(),
                populationTime
        );

        if (exists) {
            log.info("[AreaCongestionCollectService] already exists. areaCode={}, populationTime={}",
                    cityData.getAreaCode(), populationTime);
            return CollectResult.SKIPPED;
        }

        AreaCongestionRaw entity = AreaCongestionRaw.builder()
                .areaCode(cityData.getAreaCode())
                .areaName(cityData.getAreaName())
                .congestionLevel(status.getCongestionLevel())
                .congestionMessage(status.getCongestionMessage())
                .populationMin(populationMin)
                .populationMax(populationMax)
                .populationTime(populationTime)
                .forecastYn(status.getForecastYn())
                .collectedAt(OffsetDateTime.now())
                .rawPayload(null)
                .build();

        areaCongestionRawRepository.save(entity);

        log.info("[AreaCongestionCollectService] collect success. areaCode={}, populationTime={}",
                entity.getAreaCode(), entity.getPopulationTime());

        return CollectResult.SUCCESS;
    }

    private LocalDateTime parsePopulationTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("populationTime is blank");
        }
        return LocalDateTime.parse(value, POPULATION_TIME_FORMATTER);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    @Transactional
    public void collectAll() {
        List<String> TARGET_AREAS = List.of(
                "광화문·덕수궁",
                "강남역",
                "명동 관광특구"
        );

        int total = TARGET_AREAS.size();
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String areaName : TARGET_AREAS) {
            try {
                CollectResult result = collectOne(areaName);

                switch(result) {
                    case SUCCESS -> successCount++;
                    case SKIPPED -> skippedCount++;
                    case FAILED -> failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[AreaCongestionCollectService] collect failed. areaName={}", areaName, e);
            }
        }

        log.info(
                "[AreaCongestionCollectService] collectAll finished. total={}, success={}, skipped={}, failed={}",
                total,
                successCount,
                skippedCount,
                failedCount
        );
    }
}
