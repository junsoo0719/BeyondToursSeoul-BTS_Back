package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.dto.CityDataApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class SeoulCityDataApiService {

    private static final String SERVICE_NAME = "citydata"; // TODO: 실제 서비스명 최종 확인
    private final RestClient restClient = RestClient.create();

    @Value("${SEOUL_OPEN_API_KEY}")
    private String apiKey;

    public CityDataApiResponseDto fetchByArea(String areaName) {
        String url = buildUrl(areaName);
        log.info("Calling Seoul citydata API. area={}, url={}", areaName, url);

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(CityDataApiResponseDto.class);
    }

    private String buildUrl(String areaName) {
        return String.format(
                "http://openapi.seoul.go.kr:8088/%s/json/%s/1/5/%s",
                apiKey,
                SERVICE_NAME,
                areaName
        );
    }
}
