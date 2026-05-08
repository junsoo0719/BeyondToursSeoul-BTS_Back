package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.config.CityDataAreaTarget;
import com.beyondtoursseoul.bts.config.CityDataAreaTargets;
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
        FAILED
    }

    @Transactional
    public CollectResult collectOne(CityDataAreaTarget target) {
        log.info("[AreaCongestionCollectService] collect start. areaName={}", target.areaName());

        CityDataApiResponseDto response = seoulCityDataApiService.fetchByArea(target.areaName());

        if (response == null || response.getCityData() == null) {
            log.warn("[AreaCongestionCollectService] cityData is null. areaName={}", target.areaName());
            return CollectResult.FAILED;
        }

        if (response.getCityData().getLivePopulationStatuses() == null
                || response.getCityData().getLivePopulationStatuses().isEmpty()) {
            log.warn("[AreaCongestionCollectService] livePopulationStatuses is empty. areaName={}", target.areaName());
            return CollectResult.FAILED;
        }

        CityDataApiResponseDto.CityData cityData = response.getCityData();
        CityDataApiResponseDto.LivePopulationStatus status = cityData.getLivePopulationStatuses().get(0);

        LocalDateTime populationTime = parsePopulationTime(status.getPopulationTime());
        OffsetDateTime collectedAt = OffsetDateTime.now();

        AreaCongestionRaw entity = areaCongestionRawRepository.findByAreaCode(cityData.getAreaCode())
                .map(existing -> {
                    existing.updateLatest(
                            cityData.getAreaName(),
                            status.getCongestionLevel(),
                            target.latitude(),
                            target.longitude(),
                            populationTime,
                            collectedAt
                    );
                    return existing;
                })
                .orElseGet(() -> AreaCongestionRaw.builder()
                        .areaCode(cityData.getAreaCode())
                        .areaName(cityData.getAreaName())
                        .congestionLevel(status.getCongestionLevel())
                        .latitude(target.latitude())
                        .longitude(target.longitude())
                        .populationTime(populationTime)
                        .collectedAt(collectedAt)
                        .build());

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

    public void collectAll() {
        int total = CityDataAreaTargets.AREA_NAMES.size();
        int successCount = 0;
        int failedCount = 0;

        for (CityDataAreaTarget target : CityDataAreaTargets.AREA_NAMES) {
            try {
                CollectResult result = collectOne(target);

                switch(result) {
                    case SUCCESS -> successCount++;
                    case FAILED -> failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[AreaCongestionCollectService] collect failed. areaName={}", target.areaName(), e);
            }
        }

        log.info(
                "[AreaCongestionCollectService] collectAll finished. total={}, success={}, failed={}",
                total,
                successCount,
                failedCount
        );
    }
}
