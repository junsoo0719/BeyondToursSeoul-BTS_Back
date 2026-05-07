package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.dto.DetailCommonResponseDto;
import com.beyondtoursseoul.bts.dto.DetailInfoResponseDto;
import com.beyondtoursseoul.bts.dto.TourApiResponseDto;
import com.beyondtoursseoul.bts.dto.TourApiResponseDto.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class AttractionApiService {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2/areaBasedList2";
    private static final String DETAIL_COMMON_URL = "https://apis.data.go.kr/B551011/KorService2/detailCommon2";
    private static final String DETAIL_INTRO_URL = "https://apis.data.go.kr/B551011/KorService2/detailIntro2";
    private static final String SEOUL_REGION_CD = "11";
    // 행사/공연(15), 여행코스(25), 숙박(32)은 찐로컬 지수 추천 목적에 맞지 않아 수집 제외
    private static final Set<String> EXCLUDED_CONTENT_TYPE_IDS = Set.of("15", "25", "32");
    private static final String SOURCE = "TOUR_API";
    private static final int PAGE_SIZE = 1000;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${PUBLIC_DATA_API_KEY}")
    private String apiKey;

    public AttractionApiService(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    public List<Attraction> fetchSeoulAttractions() {
        List<Attraction> result = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while ((long) (pageNo - 1) * PAGE_SIZE < totalCount) {
            try {
                String json = restClient.get()
                        .uri(buildListUrl(pageNo))
                        .retrieve()
                        .body(String.class);

                TourApiResponseDto response = objectMapper.readValue(json, TourApiResponseDto.class);

                if (response == null
                        || response.getResponse() == null
                        || response.getResponse().getBody() == null) {
                    log.warn("[AttractionApi] 응답 없음 — pageNo: {}", pageNo);
                    break;
                }

                TourApiResponseDto.Body body = response.getResponse().getBody();
                totalCount = body.getTotalCount();

                if (body.getItems() == null || body.getItems().getItem() == null) {
                    log.warn("[AttractionApi] 항목 없음 — pageNo: {}", pageNo);
                    break;
                }

                List<Item> items = body.getItems().getItem();
                log.info("[AttractionApi] pageNo={}, totalCount={}, 수신={}건", pageNo, totalCount, items.size());

                for (Item item : items) {
                    toAttraction(item).ifPresent(result::add);
                }
            } catch (Exception e) {
                log.warn("[AttractionApi] 페이지 파싱 실패 pageNo={}: {}", pageNo, e.getMessage());
                break;
            }

            pageNo++;
        }

        log.info("[AttractionApi] 서울 관광지 수집 완료: {}건", result.size());
        return result;
    }

    private Optional<Attraction> toAttraction(Item item) {
        if (EXCLUDED_CONTENT_TYPE_IDS.contains(item.getContentTypeId())) {
            return Optional.empty();
        }
        if (item.getMapX() == null || item.getMapX().isBlank()
                || item.getMapY() == null || item.getMapY().isBlank()) {
            return Optional.empty();
        }

        try {
            double lng = Double.parseDouble(item.getMapX());
            double lat = Double.parseDouble(item.getMapY());
            if (lng == 0.0 || lat == 0.0) return Optional.empty();

            Point geom = GF.createPoint(new Coordinate(lng, lat));

            return Optional.of(
                    Attraction.builder()
                            .externalId(item.getContentId())
                            .name(item.getTitle())
                            .category(item.getContentTypeId())
                            .address(blankToNull(item.getAddr1()))
                            .geom(geom)
                            .source(SOURCE)
                            .createdAt(OffsetDateTime.now())
                            .thumbnail(blankToNull(item.getFirstImage()))
                            .cat1(blankToNull(item.getCat1()))
                            .cat2(blankToNull(item.getCat2()))
                            .cat3(blankToNull(item.getCat3()))
                            .tel(blankToNull(item.getTel()))
                            .build()
            );
        } catch (NumberFormatException e) {
            log.warn("[AttractionApi] 좌표 파싱 실패 contentId={}: {}", item.getContentId(), e.getMessage());
            return Optional.empty();
        }
    }

    public record CommonDetail(String overview, String tel) {}

    public CommonDetail fetchCommonDetail(String contentId) {
        try {
            String url = DETAIL_COMMON_URL
                    + "?serviceKey=" + apiKey
                    + "&MobileOS=ETC&MobileApp=BTS&_type=json"
                    + "&contentId=" + contentId;

            String json = restClient.get().uri(url).retrieve().body(String.class);
            DetailCommonResponseDto response = objectMapper.readValue(json, DetailCommonResponseDto.class);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return new CommonDetail(null, null);
            }
            DetailCommonResponseDto.Item item = response.getResponse().getBody().getItems().getItem().get(0);
            return new CommonDetail(blankToNull(item.getOverview()), blankToNull(item.getTel()));
        } catch (Exception e) {
            log.warn("[AttractionApi] detailCommon2 조회 실패 contentId={}: {}", contentId, e.getMessage());
            return new CommonDetail(null, null);
        }
    }

    public String fetchOperatingHours(String contentId, String contentTypeId) {
        try {
            String url = DETAIL_INTRO_URL
                    + "?serviceKey=" + apiKey
                    + "&MobileOS=ETC&MobileApp=BTS&_type=json"
                    + "&contentId=" + contentId
                    + "&contentTypeId=" + contentTypeId;

            String json = restClient.get().uri(url).retrieve().body(String.class);
            DetailInfoResponseDto response = objectMapper.readValue(json, DetailInfoResponseDto.class);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null
                    || response.getResponse().getBody().getItems().getItem().isEmpty()) {
                return null;
            }
            return response.getResponse().getBody().getItems().getItem().get(0).resolveOperatingHours();
        } catch (Exception e) {
            log.warn("[AttractionApi] 운영시간 조회 실패 contentId={}: {}", contentId, e.getMessage());
            return null;
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String buildListUrl(int pageNo) {
        return BASE_URL
                + "?serviceKey=" + apiKey
                + "&numOfRows=" + PAGE_SIZE
                + "&pageNo=" + pageNo
                + "&MobileOS=ETC"
                + "&MobileApp=BTS"
                + "&_type=json"
                + "&arrange=A"
                + "&lDongRegnCd=" + SEOUL_REGION_CD;
    }
}
