package com.beyondtoursseoul.bts.service.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventImage;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.*;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import com.beyondtoursseoul.bts.service.translation.GoogleTranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 관광공사 Tour API 4.0 연동 서비스
 * 서울 지역의 축제/행사 정보를 수집하고 상세 정보를 동기화합니다.
 */
@Slf4j
@Service
public class TourApiService {

    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;
    private final GoogleTranslationService translationService;
    private final RestClient restClient;

    @Value("${public.data.api.key}")
    private String tourApiKey;

    private static final String PUBLIC_DATA_API_URL =
            "https://apis.data.go.kr/B551011/";

    public TourApiService(TourApiEventRepository eventRepository,
                          TourApiEventTranslationRepository translationRepository,
                          GoogleTranslationService translationService) {
        this.eventRepository = eventRepository;
        this.translationRepository = translationRepository;
        this.translationService = translationService;
        this.restClient = RestClient.builder().baseUrl(PUBLIC_DATA_API_URL).build();
    }


    /**
     * 특정 언어에 대한 서울 지역 축제/행사 정보를 전체 동기화합니다.
     *
     * @param lang 수집할 언어 설정
     */
    @Transactional
    public void syncFestivals(TourLanguage lang) {
        log.info("서울 축제/행사 정보 전체 sync 시작, language: {}", lang);

        TourApiResponseDto<TourApiEventItemDto> response = fetchFestivalsFromApi(lang);

        if (response != null && response.getResponse().getBody().getItems() != null) {
            List<TourApiEventItemDto> items = response.getResponse().getBody().getItems().getItem();
            int successCount = 0;
            int failCount = 0;

            for (TourApiEventItemDto item : items) {
                try {
                    processSingleItem(item, lang);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[Sync Failed] contentId: {}, title: {}, error: {}",
                            item.getContentId(), item.getTitle(), e.getMessage());
                    throw e; // 하나라도 실패하면 전체 롤백을 위해 예외를 다시 던짐
                }
            }
            log.info("Sync finished for {}. Success: {}, Fail: {}", lang, successCount, failCount);
        } else {
            log.warn("Tour Api 조회 결과, 데이터가 비어있습니다. language: {}", lang);
        }
        if (lang != TourLanguage.KOR) {
            processMissingTranslations(lang);
        }

    }

    /**
     * 개별 아이템에 대한 동기화 로직을 수행합니다.
     */
    public void processSingleItem(TourApiEventItemDto dto, TourLanguage lang) {
        // 1. 기존 데이터 존재 여부 확인 및 수정 일시 비교
        TourApiEvent existingEvent = eventRepository.findById(dto.getContentId()).orElse(null);

        // 이미 존재하고, API의 수정시간이 DB의 수정시간과 같다면 상세정보 조회를 건너뜀
        if (existingEvent != null && Objects.equals(dto.getModifiedTime(), existingEvent.getModifiedTime())) {
            // 단, 해당 언어의 번역본이 아예 없는 경우는 상세정보를 가져와야 함 (목록 정보만으로는 부족)
            boolean hasTranslation = translationRepository.existsByEventAndLanguage(existingEvent, lang);
            if (hasTranslation) {
                log.info("[Sync Skip] 변경사항 없음: contentId={}, lang={}", dto.getContentId(), lang);
                // 변경이 없더라도 패치 시도 시간은 갱신해줍니다.
                existingEvent.setLastFetchAttemptTime(LocalDateTime.now());
                eventRepository.save(existingEvent);
                return;
            }
        }

        // 2. 공통 부분 정보 처리(upsert)
        TourApiEvent event = existingEvent != null ? existingEvent :
                TourApiEvent.builder().contentId(dto.getContentId()).build();
        updateEventEntity(event, dto);
        TourApiEvent managedEvent = eventRepository.save(event);

        // 3. 번역본 처리(upsert)
        TourApiEventTranslation translation = translationRepository.findByEventAndLanguage(managedEvent, lang)
                .orElseGet(() -> TourApiEventTranslation.builder()
                        .event(managedEvent)
                        .language(lang)
                        .build());

        translation.setTitle(dto.getTitle());
        translation.setAddress(dto.getAddr1() + (dto.getAddr2() != null ? " " + dto.getAddr2() : ""));

        // 공식 API에서 가져온 정보임을 명시
        translation.setIsAutoTranslated(false);
        translation.setLastTranslatedModifiedTime(dto.getModifiedTime());

        // 4. 상세 정보 동기화 (상세 설명, 축제 정보, 이미지 등 추가 API 호출)
        syncDetails(managedEvent, translation, lang);

        // 5. 저장
        translationRepository.save(translation);
    }

    /**
     * 공식 API에서 제공하지 않는 행사들에 대해 국문 데이터를 기반으로 자동 번역을 수행합니다.
     */
    private void processMissingTranslations(TourLanguage targetLang) {
        List<TourApiEvent> needingTranslation = eventRepository.findEventsNeedingTranslation(targetLang);
        log.info("[Auto Translation] 누락/만료된 번역 대상 갯수: {} (Language: {})", needingTranslation.size(), targetLang);

        // 제한된 개수만큼만 처리
        List<TourApiEvent> targetList = needingTranslation.stream().toList();

        for (TourApiEvent event : targetList) {
            try {
                translateAndSaveEvent(event, targetLang);
            } catch (Exception e) {
                log.error("[Auto Translation Error] contentId: {} 번역 중 에러 발생: {}", event.getContentId(), e.getMessage());
            }
        }
    }

    /**
     * 국문 정보를 가져와서 대상 언어로 번역 후 저장합니다.
     */
    private void translateAndSaveEvent(TourApiEvent event, TourLanguage targetLang) {
        // 국문 번역본 조회 (기준 데이터)
        TourApiEventTranslation kor = translationRepository.findByEventAndLanguage(event, TourLanguage.KOR)
                .orElse(null);

        if (kor == null) {
            log.warn("[Auto Translation] 국문 데이터가 없어 번역을 건너뜁니다. contentId: {}", event.getContentId());
            return;
        }

        TourApiEventTranslation target = translationRepository.findByEventAndLanguage(event, targetLang)
                .orElseGet(() -> TourApiEventTranslation.builder()
                        .event(event)
                        .language(targetLang)
                        .build());

        String sourceCode = TourLanguage.KOR.getGoogleLangCode();
        String targetCode = targetLang.getGoogleLangCode();

        log.info("[Auto Translation] 번역 시작: contentId={}, {} -> {}", event.getContentId(), sourceCode, targetCode);

        // 1. 번역할 필드들을 리스트로 구성
        List<String> sourceTexts = List.of(
                nullToEmpty(kor.getTitle()),
                nullToEmpty(kor.getAddress()),
                nullToEmpty(kor.getOverview()),
                nullToEmpty(kor.getEventPlace()),
                nullToEmpty(kor.getProgram()),
                nullToEmpty(kor.getSubEvent()),
                nullToEmpty(kor.getUseTimeFestival())
        );

        // 2. 한 번의 호출로 배치 번역 수행
        List<String> translatedTexts = translationService.translateBatch(sourceTexts, sourceCode, targetCode);

        // 3. 결과 매핑 (결과가 비어있지 않고 원본과 갯수가 맞을 경우)
        if (translatedTexts.size() == sourceTexts.size()) {
            target.setTitle(translatedTexts.get(0));
            target.setAddress(translatedTexts.get(1));
            target.setOverview(translatedTexts.get(2));
            target.setEventPlace(translatedTexts.get(3));
            target.setProgram(translatedTexts.get(4));
            target.setSubEvent(translatedTexts.get(5));
            target.setUseTimeFestival(translatedTexts.get(6));
        } else {
            log.error("[Auto Translation Error] 번역 결과가 누락되었습니다. 개별 번역으로 시도하거나 건너뜁니다. contentId: {}", event.getContentId());
            return;
        }

        // 단순 정보는 복사 (이미 번역된 것이 아님)
        target.setHomepage(kor.getHomepage());
        target.setTelName(kor.getTelName());
        target.setPlayTime(kor.getPlayTime());
        target.setAgeLimit(kor.getAgeLimit());
        target.setBookingPlace(kor.getBookingPlace());
        target.setDiscountInfoFestival(kor.getDiscountInfoFestival());
        target.setSpendTimeFestival(kor.getSpendTimeFestival());
        target.setFestivalGrade(kor.getFestivalGrade());
        target.setSponsor1(kor.getSponsor1());
        target.setSponsor1tel(kor.getSponsor1tel());
        target.setSponsor2(kor.getSponsor2());
        target.setSponsor2tel(kor.getSponsor2tel());

        // 메타 데이터 설정
        target.setIsAutoTranslated(true);
        target.setLastTranslatedModifiedTime(event.getModifiedTime());

        translationRepository.save(target);
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * 테스트용: 첫 번째 한 건의 데이터만 상세 정보까지 포함하여 동기화합니다.
     *
     * @param lang 수집할 언어 설정
     */
    @Transactional
    public void syncOneFestival(TourLanguage lang) {
        log.info("서울 축제/행사 정보 단건 sync 테스트 시작, language: {}", lang);

        TourApiResponseDto<TourApiEventItemDto> response = fetchFestivalsFromApi(lang);
        if (response == null || response.getResponse().getBody().getItems() == null || response.getResponse()
                .getBody()
                .getItems()
                .getItem()
                .isEmpty()) {
            log.warn("조회된 데이터가 없습니다.");
            return;
        }

        TourApiEventItemDto item = response.getResponse().getBody().getItems().getItem().get(0);
        try {
            processSingleItem(item, lang);
            log.info("단건 동기화 완료: {}", item.getTitle());
        } catch (Exception e) {
            log.error("[Sync One Failed] contentId: {}, title: {}, error: {}",
                    item.getContentId(), item.getTitle(), e.getMessage());
        }
    }

    /**
     * 콘텐츠 ID를 기반으로 공통/소개/이미지 상세 정보를 각각 호출하여 엔티티에 병합합니다.
     */
    private void syncDetails(TourApiEvent event, TourApiEventTranslation translation, TourLanguage lang) {
        Long contentId = event.getContentId();
        Long contentTypeId = event.getContentTypeId();
        log.info("[Detail Sync] 시작 - contentId: {}, contentTypeId: {}, lang: {}", contentId, contentTypeId, lang);

        // 1. 공통 정보 조회 (개요, 홈페이지, 전화번호 명칭 등)
        TourApiDetailCommonItemDto commonDto = fetchDetailCommon(contentId, lang);
        if (commonDto != null) {
            translation.setOverview(commonDto.getOverview());
            translation.setHomepage(commonDto.getHomepage());
            translation.setTelName(commonDto.getTelName());
        }

        // 2. 소개 정보 조회 (축제 전용 상세 정보: 장소, 시간, 요금, 주최자 등)
        TourApiDetailIntroItemDto introDto = fetchDetailIntro(contentId, contentTypeId, lang);
        if (introDto != null) {
            translation.setEventPlace(introDto.getEventPlace());
            translation.setPlayTime(introDto.getPlayTime());
            translation.setUseTimeFestival(introDto.getUseTimeFestival());
            translation.setProgram(introDto.getProgram());
            translation.setAgeLimit(introDto.getAgeLimit());
            translation.setBookingPlace(introDto.getBookingPlace());
            translation.setSubEvent(introDto.getSubEvent());
            translation.setDiscountInfoFestival(introDto.getDiscountInfoFestival());
            translation.setSpendTimeFestival(introDto.getSpendTimeFestival());
            translation.setFestivalGrade(introDto.getFestivalGrade());
            translation.setSponsor1(introDto.getSponsor1());
            translation.setSponsor1tel(introDto.getSponsor1tel());
            translation.setSponsor2(introDto.getSponsor2());
            translation.setSponsor2tel(introDto.getSponsor2tel());
        }

        // 3. 이미지 정보 조회 (KOR 일 때만 업데이트하여 데이터 유실 방지)
        // 국문 데이터가 가장 풍부하므로 국문 동기화 시에만 이미지 갱신을 수행합니다.
        if (lang == TourLanguage.KOR) {
            List<TourApiDetailImageItemDto> imageDtos = fetchDetailImages(contentId, lang);
            if (imageDtos != null && !imageDtos.isEmpty()) {
                log.info("[Detail Sync] 이미지 정보 갱신 (이미지 개수: {})", imageDtos.size());
                event.getImages().clear();
                for (TourApiDetailImageItemDto imgDto : imageDtos) {
                    TourApiEventImage image = TourApiEventImage.builder()
                            .event(event)
                            .originImgUrl(imgDto.getOriginImgUrl())
                            .smallImgUrl(imgDto.getSmallImgUrl())
                            .copyrightType(imgDto.getCopyrightType())
                            .build();
                    event.getImages().add(image);
                }
            }
        }
    }

    /**
     * [API] 공통 정보 상세 조회 (detailcommon2)
     */
    private TourApiDetailCommonItemDto fetchDetailCommon(Long contentId, TourLanguage lang) {
        String servicePath = lang.getServiceName() + "/detailCommon2";
        log.info("[API Request] DetailCommon - path: {}, contentId: {}", servicePath, contentId);

        try {
            TourApiResponseDto<TourApiDetailCommonItemDto> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(servicePath)
                            .queryParam("serviceKey", tourApiKey)
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "BeyondToursSeoul")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
//                            .queryParam("defaultYN", "Y")
//                            .queryParam("firstImageYN", "N")
//                            .queryParam("addrYN", "N")
//                            .queryParam("mapYN", "N")
//                            .queryParam("overviewYN", "Y")
                            .build())
                    .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiDetailCommonItemDto>>() {
                    });

            if (response != null && response.getResponse().getBody().getItems() != null && !response.getResponse()
                    .getBody()
                    .getItems()
                    .getItem()
                    .isEmpty()) {
                return response.getResponse().getBody().getItems().getItem().get(0);
            }
        } catch (Exception e) {
            log.error("[API Error] DetailCommon 호출 실패: {}", e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * [API] 소개 정보 상세 조회 (detailintro2)
     */
    private TourApiDetailIntroItemDto fetchDetailIntro(Long contentId, Long contentTypeId, TourLanguage lang) {
        String servicePath = lang.getServiceName() + "/detailIntro2";
        log.info("[API Request] DetailIntro - path: {}, contentId: {}", servicePath, contentId);

        try {
            TourApiResponseDto<TourApiDetailIntroItemDto> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(servicePath)
                            .queryParam("serviceKey", tourApiKey)
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "BeyondToursSeoul")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .queryParam("contentTypeId", contentTypeId)
                            .build())
                    .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiDetailIntroItemDto>>() {
                    });

            if (response != null && response.getResponse().getBody().getItems() != null && !response.getResponse()
                    .getBody()
                    .getItems()
                    .getItem()
                    .isEmpty()) {
                return response.getResponse().getBody().getItems().getItem().get(0);
            }
        } catch (Exception e) {
            log.error("[API Error] DetailIntro 호출 실패: {}", e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * [API] 이미지 정보 상세 조회 (detailimage2)
     */
    private List<TourApiDetailImageItemDto> fetchDetailImages(Long contentId, TourLanguage lang) {
        String servicePath = lang.getServiceName() + "/detailImage2";
        log.info("[API Request] DetailImage - path: {}, contentId: {}", servicePath, contentId);

        try {
            TourApiResponseDto<TourApiDetailImageItemDto> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(servicePath)
                            .queryParam("serviceKey", tourApiKey)
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "BeyondToursSeoul")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .queryParam("imageYN", "Y")
                            .build())
                    .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiDetailImageItemDto>>() {
                    });

            if (response != null && response.getResponse().getBody().getItems() != null) {
                return response.getResponse().getBody().getItems().getItem();
            }

        } catch (HttpMessageConversionException | RestClientException e) {
            // Tour API 특성상 이미지가 없으면 "items": "" 형태로 응답하여 파싱 에러가 발생함
            log.warn("[API Warn] 상세 이미지가 없습니다 (파싱 에러 처리). contentId: {}", contentId);
            log.warn("[API Warn] 에러메시지: {}", e.getMessage());
            return List.of(); // 빈 리스트를 반환하여 로직이 계속 진행되도록 함

        } catch (Exception e) {
            // 그 외의 진짜 통신 에러 등
            log.error("[API Error] DetailImage 호출 실패 - contentId: {}, 에러: {}", contentId, e.getMessage());
            return List.of(); // 예외를 던지지 않고 빈 리스트를 반환해 다른 상세정보라도 저장되도록 처리
        }

        return List.of();
    }

    /**
     * API 원본 데이터를 DTO로 반환 (테스트 및 모니터링용)
     */
    public TourApiResponseDto<TourApiEventItemDto> getRawFestivals(TourLanguage lang) {
        return fetchFestivalsFromApi(lang);
    }

    /**
     * [API] 행사 목록 조회 (searchFestival2)
     */
    private TourApiResponseDto<TourApiEventItemDto> fetchFestivalsFromApi(TourLanguage lang) {
        String servicePath = lang.getServiceName() + "/searchFestival2";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(servicePath)
                        .queryParam("serviceKey", tourApiKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "BeyondToursSeoul")
                        .queryParam("_type", "json")
                        .queryParam("numOfRows", 100)
                        .queryParam("eventStartDate", today)
                        .queryParam("lDongRegnCd", "11") // 서울 법정동 코드
                        .build())
                .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiEventItemDto>>() {
                });
    }

    /**
     * 엔티티의 공통 정보(ID, 이미지, 좌표 등)를 DTO 기반으로 갱신합니다.
     */
    private void updateEventEntity(TourApiEvent event, TourApiEventItemDto dto) {
        event.setContentTypeId(dto.getContentTypeId());
        event.setFirstImage(dto.getFirstImage());
        event.setFirstImage2(dto.getFirstImage2());
        event.setMapX(dto.getMapX());
        event.setMapY(dto.getMapY());
        event.setTel(dto.getTel());
        event.setZipCode(dto.getZipCode());
        event.setEventStartDate(dto.getEventStartDate());
        event.setEventEndDate(dto.getEventEndDate());
        event.setModifiedTime(dto.getModifiedTime());
        event.setLastSyncTime(LocalDateTime.now());
        event.setLastFetchAttemptTime(LocalDateTime.now());
    }

}
