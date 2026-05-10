package com.beyondtoursseoul.bts.runner;

import com.beyondtoursseoul.bts.repository.DongBoundaryRepository;
import com.beyondtoursseoul.bts.service.DongBoundaryImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DongBoundaryImportRunner implements ApplicationRunner {

    private final DongBoundaryImportService dongBoundaryImportService;
    private final DongBoundaryRepository dongBoundaryRepository;

    private static final String GEOJSON_PATH = "geojson/seoul_dong.geojson";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (dongBoundaryRepository.count() > 0) {
            log.info("행정동 경계 데이터가 이미 존재합니다. import를 건너뜁니다.");
            return;
        }

        ClassPathResource resource = new ClassPathResource(GEOJSON_PATH);
        if (!resource.exists()) {
            log.warn("GeoJSON 파일을 찾을 수 없습니다: {}", GEOJSON_PATH);
            return;
        }

        log.info("행정동 경계 데이터 import를 시작합니다.");
        try (InputStream inputStream = resource.getInputStream()) {
            dongBoundaryImportService.importFromGeoJson(inputStream);
        }
    }
}
