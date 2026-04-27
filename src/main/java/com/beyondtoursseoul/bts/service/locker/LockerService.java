package com.beyondtoursseoul.bts.service.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import com.beyondtoursseoul.bts.dto.locker.LockerResponseDto;
import com.beyondtoursseoul.bts.repository.locker.LockerRepository;
import com.beyondtoursseoul.bts.service.translation.LockerTranslationService;
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
     * 물품보관함 api 호출 및 DTO 변환
     */
    public LockerResponseDto fetchLockerData() {
        // 요청 url
        String apiUrl = String.format("http://openapi.seoul.go.kr:8088/%s/json/getFcLckr/1/2/", seoulOpenApiKey);
        log.info("Calling Locker API: {}", apiUrl);

        // DTO 클래스로 파싱
        LockerResponseDto responseDto = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(LockerResponseDto.class);

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
        LockerResponseDto responseDto = fetchLockerData();

        // null값 처리
        if (responseDto == null || responseDto.getResponse().getBody().getItems() == null) {
            log.warn("저장할 물품보관함 데이터가 없습니다.");
            return;
        }

        List<LockerResponseDto.Item> rawItems = responseDto.getResponse().getBody().getItems().getItem();

        // Map을 사용하여 lckrId를 기준으로 중복된 데이터 제거
        Map<String, LockerResponseDto.Item> uniqueItemsMap = new HashMap<>();
        for (LockerResponseDto.Item item : rawItems) {
            // putIfAbsent: 만약 같은 lckrId가 여러 번 들어오면 첫 번째 것만 남기고 무시
            uniqueItemsMap.putIfAbsent(item.getLckrId(), item);
        }

        // 하나씩 순회하며 DB에 반영(Upsert)
        for (LockerResponseDto.Item item : uniqueItemsMap.values()) {
            
            // 추가 요금 단위 시간이 숫자가 아닐 경우를 대비해 안전하게 변환합니다.
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

        /// 번역도 추가적으로 진행해서 전체 패치
        lockerTranslationService.translateAllKoToMultiLang();
    }

}