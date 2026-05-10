package com.beyondtoursseoul.bts.runner;

import com.beyondtoursseoul.bts.domain.TourCategory;
import com.beyondtoursseoul.bts.dto.CategoryCodeResponseDto;
import com.beyondtoursseoul.bts.repository.TourCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
//@Component
@Order(1)
public class CategoryInitRunner implements ApplicationRunner {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2/lclsSystmCode2";

    private final TourCategoryRepository repository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${PUBLIC_DATA_API_KEY}")
    private String apiKey;

    public CategoryInitRunner(TourCategoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("[CategoryInit] 카테고리 데이터 이미 존재 — 스킵");
            return;
        }

        log.info("[CategoryInit] 카테고리 코드 수집 시작");
        List<TourCategory> all = new ArrayList<>();

        List<CategoryCodeResponseDto.Item> lv1List = fetchCategories(null, null);
        log.info("[CategoryInit] lv1 {}건 수신", lv1List.size());
        for (CategoryCodeResponseDto.Item lv1 : lv1List) {
            if (lv1.getCode() == null) { log.warn("[CategoryInit] lv1 code null, name={}", lv1.getName()); continue; }
            all.add(TourCategory.builder().code(lv1.getCode()).name(lv1.getName()).level(1).build());

            List<CategoryCodeResponseDto.Item> lv2List = fetchCategories(lv1.getCode(), null);
            log.info("[CategoryInit] lv2 of {} — {}건 수신", lv1.getCode(), lv2List.size());
            for (CategoryCodeResponseDto.Item lv2 : lv2List) {
                if (lv2.getCode() == null) { log.warn("[CategoryInit] lv2 code null, name={}", lv2.getName()); continue; }
                all.add(TourCategory.builder().code(lv2.getCode()).name(lv2.getName()).level(2).build());

                List<CategoryCodeResponseDto.Item> lv3List = fetchCategories(lv1.getCode(), lv2.getCode());
                log.info("[CategoryInit] lv3 of {}/{} — {}건 수신", lv1.getCode(), lv2.getCode(), lv3List.size());
                for (CategoryCodeResponseDto.Item lv3 : lv3List) {
                    if (lv3.getCode() == null) { log.warn("[CategoryInit] lv3 code null, name={}", lv3.getName()); continue; }
                    all.add(TourCategory.builder().code(lv3.getCode()).name(lv3.getName()).level(3).build());
                }
            }
        }

        repository.saveAll(all);
        long lv1Count = all.stream().filter(c -> c.getLevel() == 1).count();
        long lv2Count = all.stream().filter(c -> c.getLevel() == 2).count();
        long lv3Count = all.stream().filter(c -> c.getLevel() == 3).count();
        log.info("[CategoryInit] 저장 완료 — lv1:{}, lv2:{}, lv3:{}, 합계:{}", lv1Count, lv2Count, lv3Count, all.size());
    }

    private List<CategoryCodeResponseDto.Item> fetchCategories(String cat1, String cat2) {
        try {
            String url = BASE_URL
                    + "?serviceKey=" + apiKey
                    + "&MobileOS=ETC&MobileApp=BTS&_type=json"
                    + (cat1 != null ? "&lclsSystm1=" + cat1 : "")
                    + (cat2 != null ? "&lclsSystm2=" + cat2 : "");

            String json = restClient.get().uri(url).retrieve().body(String.class);
            CategoryCodeResponseDto response = objectMapper.readValue(json, CategoryCodeResponseDto.class);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null) {
                return List.of();
            }
            return response.getResponse().getBody().getItems().getItem();
        } catch (Exception e) {
            log.warn("[CategoryInit] 카테고리 조회 실패 cat1={} cat2={}: {}", cat1, cat2, e.getMessage());
            return List.of();
        }
    }
}
