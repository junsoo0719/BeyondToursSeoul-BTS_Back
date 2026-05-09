package com.beyondtoursseoul.bts.service.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.locker.LockerApiResponseDto;
import com.beyondtoursseoul.bts.dto.locker.LockerDetailResponse;
import com.beyondtoursseoul.bts.dto.locker.LockerNearestEntryResponse;
import com.beyondtoursseoul.bts.dto.locker.LockerSummaryResponse;
import com.beyondtoursseoul.bts.repository.locker.LockerRepository;
import com.beyondtoursseoul.bts.service.translation.LockerTranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 물품보관함 서비스 클래스
 **/

@Slf4j
@Service
public class LockerService {

    // http 클라이언트
    private final RestClient restClient;
    private final LockerRepository lockerRepository;
    private final LockerTranslationService lockerTranslationService;

    // api값 가져옴
    @Value("${seoul.open-api.key}")
    private String seoulOpenApiKey;

    public LockerService(LockerRepository lockerRepository, LockerTranslationService lockerTranslationService) {
        this.restClient = RestClient.create();
        this.lockerRepository = lockerRepository;
        this.lockerTranslationService = lockerTranslationService;
    }

    /**
     * 특정 언어에 맞는 물품보관함 리스트를 조회합니다. (지도/핀용)
     */
    @Transactional(readOnly = true)
    public List<LockerSummaryResponse> getLockerList(TourLanguage lang) {
        String langCode = lang.getLockerLangCode();
        return lockerRepository.findAll().stream()
                .map(locker -> {
                    LockerTranslation translation = findTranslation(locker, langCode);
                    return translation != null ? new LockerSummaryResponse(locker, translation) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 주어진 좌표에서 가장 가까운 물품보관함(직선거리) 목록. 좌표 없는 행은 제외.
     */
    @Transactional(readOnly = true)
    public List<LockerNearestEntryResponse> findNearestLockers(
            double latitude,
            double longitude,
            int limit,
            TourLanguage lang
    ) {
        int cap = Math.min(Math.max(limit, 1), 20);
        List<LockerSummaryResponse> all = getLockerList(lang);
        return all.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    double km = haversineKm(latitude, longitude, s.getLatitude(), s.getLongitude());
                    return new LockerNearestEntryResponse(s, km * 1000.0);
                })
                .sorted(Comparator.comparingInt(LockerNearestEntryResponse::getDistanceMeters))
                .limit(cap)
                .collect(Collectors.toList());
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    /**
     * 특정 언어에 맞는 물품보관함 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public LockerDetailResponse getLockerDetail(Long id, TourLanguage lang) {
        Locker locker = lockerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다. ID: " + id));

        String langCode = lang.getLockerLangCode();
        LockerTranslation translation = findTranslation(locker, langCode);

        if (translation == null) {
            throw new IllegalStateException("해당 보관함의 번역 데이터가 존재하지 않습니다.");
        }

        return new LockerDetailResponse(locker, translation);
    }

    /**
     * 보관함에서 특정 언어의 번역본을 찾고, 없으면 한국어('ko')를 반환합니다.
     */
    private LockerTranslation findTranslation(Locker locker, String langCode) {
        return locker.getTranslations().stream()
                .filter(t -> langCode.equals(t.getLanguageCode()))
                .findFirst()
                .orElseGet(() -> locker.getTranslations().stream()
                        .filter(t -> "ko".equals(t.getLanguageCode()))
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 물품보관함 api 호출 및 DTO 변환
     */
    public LockerApiResponseDto fetchLockerData() {
        // 요청 url
        String apiUrl = String.format("http://openapi.seoul.go.kr:8088/%s/json/getFcLckr/1/3/", seoulOpenApiKey);
        log.info("Calling Locker API: {}", apiUrl);

        // DTO 클래스로 파싱
        LockerApiResponseDto responseDto = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(LockerApiResponseDto.class);

        // 변경된 DTO 구조에 맞게 로그 출력 수정
        if (responseDto != null && responseDto.getResponse() != null
                && responseDto.getResponse().getBody() != null) {
            log.info("성공적으로 데이터를 파싱했습니다. 개수: {}",
                    responseDto.getResponse().getBody().getNumOfRows());
        }

        return responseDto;
    }

    /**
     * 물품보관함 데이터 최신화 (2주에 1번 호출)
     */
    @Transactional
    public void syncLockerDataToDb() {
        // 1. API 응답 데이터 가져오기
        LockerApiResponseDto responseDto = fetchLockerData();

        // null값 처리
        if (responseDto == null || responseDto.getResponse() == null || responseDto.getResponse().getBody() == null || responseDto.getResponse().getBody().getItems() == null) {
            log.warn("저장할 물품보관함 데이터가 없습니다.");
            return;
        }

        List<LockerApiResponseDto.Item> rawItems = responseDto.getResponse().getBody().getItems().getItem();

        // Map을 사용하여 lckrId를 기준으로 중복된 데이터 제거
        Map<String, LockerApiResponseDto.Item> uniqueItemsMap = new HashMap<>();
        for (LockerApiResponseDto.Item item : rawItems) {
            // putIfAbsent: 만약 같은 lckrId가 여러 번 들어오면 첫 번째 것만 남기고 무시
            uniqueItemsMap.putIfAbsent(item.getLockerId(), item);
        }

        // 하나씩 순회하며 DB에 반영(Upsert)
        for (LockerApiResponseDto.Item item : uniqueItemsMap.values()) {
            
            // 추가 요금 단위 시간이 숫자가 아닐 경우를 대비해 안전하게 변환합니다.
            int addChargeUnit = 60;
            try {
                if (item.getAddChargeUnit() != null && !item.getAddChargeUnit().isBlank()) {
                    addChargeUnit = Integer.parseInt(item.getAddChargeUnit());
                }
            } catch (NumberFormatException e) {
                log.warn("보관함 ID {}의 추가 요금 단위 시간 형식이 올바르지 않습니다: {}", item.getLockerId(), item.getAddChargeUnit());
            }

            // DB에 해당 보관함이 이미 존재하는지 찾아봅니다.
            Optional<Locker> optionalLocker = lockerRepository.findByLckrId(item.getLockerId());
            Locker locker;

            if (optionalLocker.isPresent()) {
                // [UPDATE] 이미 존재한다면, 엔티티의 값을 최신 데이터로 덮어씁니다.
                locker = optionalLocker.get();
                locker.update(
                        item.getLatitude(),
                        item.getLongitude(),
                        item.getTotalCount(),
                        item.getWeekdayStartTime(),
                        item.getWeekdayEndTime(),
                        item.getWeekendStartTime(),
                        item.getWeekendEndTime(),
                        addChargeUnit
                );
                log.info("덮어씌워진 id:{}", item.getLockerId());
            } else {
                // [INSERT] DB에 없는 새로운 보관함이라면, 새로 객체를 생성합니다.
                locker = Locker.builder()
                        .lckrId(item.getLockerId())
                        .latitude(item.getLatitude())
                        .longitude(item.getLongitude())
                        .totalCnt(item.getTotalCount())
                        .weekdayStartTime(item.getWeekdayStartTime())
                        .weekdayEndTime(item.getWeekdayEndTime())
                        .weekendStartTime(item.getWeekendStartTime())
                        .weekendEndTime(item.getWeekendEndTime())
                        .addChargeUnit(addChargeUnit)
                        .build();
                log.info("새로만들어진 id: {}", item.getLockerId());
            }

            // 3. 한국어 번역본 처리 (있으면 업데이트, 없으면 신규 생성)
            String combinedSizeInfo = String.format("가로: %s, 깊이: %s, 높이: %s", 
                    item.getLockerWidth(), item.getLockerDepth(), item.getLockerHeight());

            Optional<LockerTranslation> existingKo = locker.getTranslations().stream()
                    .filter(t -> "ko".equals(t.getLanguageCode()))
                    .findFirst();

            if (existingKo.isPresent()) {
                // 기존 번역 업데이트
                existingKo.get().update(
                        item.getStationName(),
                        item.getLockerName(),
                        item.getLockerDetailLocation(),
                        item.getBasicChargeInfo(),
                        item.getAddChargeInfo(),
                        combinedSizeInfo,
                        item.getKeepLimitInfo()
                );
            } else {
                // 신규 번역 생성 (빌더 내부에서 자동으로 locker의 리스트에 추가됨)
                LockerTranslation.builder()
                        .locker(locker)
                        .languageCode("ko")
                        .stationName(item.getStationName())
                        .lockerName(item.getLockerName())
                        .detailLocation(item.getLockerDetailLocation())
                        .basePriceInfo(item.getBasicChargeInfo())
                        .addPriceInfo(item.getAddChargeInfo())
                        .sizeInfo(combinedSizeInfo)
                        .limitItemsInfo(item.getKeepLimitInfo())
                        .build();
            }

            // 4. DB에 저장 (JPA Dirty Checking에 의해 변경사항이 반영되며, 신규일 경우 insert 수행)
            lockerRepository.save(locker);
        }

        log.info("성공적으로 {}개의 물품보관함 데이터를 최신화했습니다!", uniqueItemsMap.size());

        /// 번역도 추가적으로 진행해서 전체 패치
        lockerTranslationService.translateAllKoToMultiLang();
    }

}
