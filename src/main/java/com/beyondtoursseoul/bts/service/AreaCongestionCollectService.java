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

    @Transactional
    public void collectOne(String areaName) {
        log.info("[AreaCongestionCollectService] collect start. areaName={}", areaName);

        CityDataApiResponseDto response = seoulCityDataApiService.fetchByArea(areaName);

        if (response == null || response.getCityData() == null) {
            log.warn("[AreaCongestionCollectService] cityData is null. areaName={}", areaName);
            return;
        }

        if (response.getCityData().getLivePopulationStatuses() == null
                || response.getCityData().getLivePopulationStatuses().isEmpty()) {
            log.warn("[AreaCongestionCollectService] livePopulationStatuses is empty. areaName={}", areaName);
            return;
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
            return;
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
        List<String> targetAreas = List.of(
                "광화문·덕수궁"
                // TODO: 나머지 장소 추가
        );

        for (String areaName : targetAreas) {
            try {
                collectOne(areaName);
            } catch (Exception e) {
                log.error("[AreaCongestionCollectService] collect failed. areaName={}", areaName, e);
            }
        }
    }
}
