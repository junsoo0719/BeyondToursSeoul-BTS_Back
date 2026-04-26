package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Locker;
import com.beyondtoursseoul.bts.domain.LockerTranslation;
import com.beyondtoursseoul.bts.dto.LockerApiResponseDto;
import com.beyondtoursseoul.bts.repository.LockerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 물품보관함 서비스 클래스
 **/

@Slf4j
@Service
public class LockerApiService {

    // http 클라이언트
    private final RestClient restClient;
    private final LockerRepository lockerRepository;

    // api값 가져옴
    @Value("${SEOUL_OPEN_API_KEY}")
    private String seoulOpenApiKey;

    public LockerApiService(LockerRepository lockerRepository) {
        this.restClient = RestClient.create();
        this.lockerRepository = lockerRepository;
    }


    /**
     * 물품보관함 api 호출 및 DTO 변환
     */
    public LockerApiResponseDto fetchLockerData() {
        // 요청 url
        String apiUrl = String.format("http://openapi.seoul.go.kr:8088/%s/json/getFcLckr/1/400/", seoulOpenApiKey);
        log.info("Calling Locker API: {}", apiUrl);

        // DTO 클래스로 파싱
        LockerApiResponseDto responseDto = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(LockerApiResponseDto.class);

        // 변경된 DTO 구조에 맞게 로그 출력 수정
        if (responseDto != null && responseDto.getResponse() != null
                && responseDto.getResponse().getBody() != null) {
            log.info("성공적으로 데이터를 파싱했습니다. 총 개수: {}",
                    responseDto.getResponse().getBody().getTotalCount());
        }

        return responseDto;
    }

    /**
     * 물품보관함 데이터 최신화 (하루 1번 호출용)
     */
    @Transactional
    public void syncLockerDataToDb() {
        // 1. API 응답 데이터 가져오기
        LockerApiResponseDto responseDto = fetchLockerData();

        // null값 처리
        if (responseDto == null || responseDto.getResponse().getBody().getItems() == null) {
            log.warn("저장할 물품보관함 데이터가 없습니다.");
            return;
        }

        List<LockerApiResponseDto.Item> rawItems = responseDto.getResponse().getBody().getItems().getItem();

        // [필수 방어] 서울시 데이터 자체에 중복된 ID가 섞여서 오는 경우가 있습니다.
        // Map을 사용하여 lckrId를 기준으로 중복된 데이터를 하나로 깔끔하게 정리합니다.
        Map<String, LockerApiResponseDto.Item> uniqueItemsMap = new HashMap<>();
        for (LockerApiResponseDto.Item item : rawItems) {
            // putIfAbsent: 만약 같은 lckrId가 여러 번 들어오면 첫 번째 것만 남기고 무시합니다.
            uniqueItemsMap.putIfAbsent(item.getLckrId(), item);
        }

        // 2. 중복이 제거된 깔끔한 데이터들을 하나씩 순회하며 DB에 반영(Upsert)합니다.
        for (LockerApiResponseDto.Item item : uniqueItemsMap.values()) {
            
            // [방어 로직] 추가 요금 단위 시간이 숫자가 아닐 경우를 대비해 안전하게 변환합니다.
            int addChargeUnit = 60;
            try {
                if (item.getAddCrgUnitHr() != null && !item.getAddCrgUnitHr().isBlank()) {
                    addChargeUnit = Integer.parseInt(item.getAddCrgUnitHr());
                }
            } catch (NumberFormatException e) {
                log.warn("보관함 ID {}의 추가 요금 단위 시간 형식이 올바르지 않습니다: {}", item.getLckrId(), item.getAddCrgUnitHr());
            }

            // DB에 해당 보관함이 이미 존재하는지 찾아봅니다.
            Optional<Locker> optionalLocker = lockerRepository.findByLckrId(item.getLckrId());
            Locker locker;

            if (optionalLocker.isPresent()) {
                // [UPDATE] 이미 존재한다면, 엔티티의 값을 최신 데이터로 덮어씁니다.
                locker = optionalLocker.get();
                locker.update(
                        item.getLat(),
                        item.getLot(),
                        item.getLckrCnt(),
                        item.getWkdayOperBgngTm(),
                        item.getWkdayOperEndTm(),
                        item.getSatOperBgngTm(),
                        item.getSatOperEndTm(),
                        addChargeUnit
                );
                log.info("덮어씌워진 id:{}", item.getLckrId());
            } else {
                // [INSERT] DB에 없는 새로운 보관함이라면, 새로 객체를 생성합니다.
                locker = Locker.builder()
                        .lckrId(item.getLckrId())
                        .latitude(item.getLat())
                        .longitude(item.getLot())
                        .totalCnt(item.getLckrCnt())
                        .weekdayStartTime(item.getWkdayOperBgngTm())
                        .weekdayEndTime(item.getWkdayOperEndTm())
                        .weekendStartTime(item.getSatOperBgngTm())
                        .weekendEndTime(item.getSatOperEndTm())
                        .addChargeUnit(addChargeUnit)
                        .build();
                log.info("새로만들어진 id: {}", item.getLckrId());
            }

            // 3. 한국어 번역본 처리 (있으면 업데이트, 없으면 신규 생성)
            String combinedSizeInfo = String.format("가로: %s, 깊이: %s, 높이: %s", 
                    item.getLckrWdthLenExpln(), item.getLckrDpthExpln(), item.getLckrHgtExpln());

            Optional<LockerTranslation> existingKo = locker.getTranslations().stream()
                    .filter(t -> "ko".equals(t.getLanguageCode()))
                    .findFirst();

            if (existingKo.isPresent()) {
                // 기존 번역 업데이트
                existingKo.get().update(
                        item.getStnNm(),
                        item.getLckrNm(),
                        item.getLckrDtlLocNm(),
                        item.getUtztnCrgExpln(),
                        item.getAddCrgExpln(),
                        combinedSizeInfo,
                        item.getKpngLmtLckrExpln()
                );
            } else {
                // 신규 번역 생성 (빌더 내부에서 자동으로 locker의 리스트에 추가됨)
                LockerTranslation.builder()
                        .locker(locker)
                        .languageCode("ko")
                        .stationName(item.getStnNm())
                        .lockerName(item.getLckrNm())
                        .detailLocation(item.getLckrDtlLocNm())
                        .basePriceInfo(item.getUtztnCrgExpln())
                        .addPriceInfo(item.getAddCrgExpln())
                        .sizeInfo(combinedSizeInfo)
                        .limitItemsInfo(item.getKpngLmtLckrExpln())
                        .build();
            }

            // 4. DB에 저장 (JPA Dirty Checking에 의해 변경사항이 반영되며, 신규일 경우 insert 수행)
            lockerRepository.save(locker);
        }

        log.info("성공적으로 {}개의 물품보관함 데이터를 최신화했습니다!", uniqueItemsMap.size());
    }

}