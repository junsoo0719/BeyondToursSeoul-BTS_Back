package com.beyondtoursseoul.bts.service.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventImage;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.*;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventImageRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 관광공사 Tour API 4.0 연동 서비스
 * 서울 지역의 축제/행사 정보를 수집하고 상세 정보를 동기화합니다.
 */
@Slf4j
@Service
public class TourApiService_ {

    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;
    private final TourApiEventImageRepository imageRepository;
    private final RestClient restClient;

    @Value("${public.data.api.key}")
    private String tourApiKey;

    private static final String PUBLIC_DATA_API_URL =
            "https://apis.data.go.kr/B551011/";

    public TourApiService_(TourApiEventRepository eventRepository,
                           TourApiEventTranslationRepository translationRepository,
                           TourApiEventImageRepository imageRepository) {
        this.eventRepository = eventRepository;
        this.translationRepository = translationRepository;
        this.imageRepository = imageRepository;
        this.restClient = RestClient.builder().baseUrl(PUBLIC_DATA_API_URL).build();
    }


    /**
     * 특정 언어에 대한 서울 지역 축제/행사 정보를 전체 동기화합니다.
     * @param lang 수집할 언어 설정
     */
    @Transactional
    public void syncFestivals(TourLanguage lang) {
        log.info("서울 축제/행사 정보 전체 sync 시작, language: {}", lang);

        TourApiResponseDto<TourApiEventItemDto> response = fetchFestivalsFromApi(lang);

        if (response == null || response.getResponse().getBody().getItems() == null) {
            log.warn("Tour Api 조회 결과, 데이터가 비어있습니다. language: {}", lang);
            return;
        }

        List<TourApiEventItemDto> items = response.getResponse().getBody().getItems().getItem();
        processItems(items, lang);

        log.info("Successfully synced {} items for language: {}", items.size(), lang);
    }

    /**
     * 테스트용: 첫 번째 한 건의 데이터만 상세 정보까지 포함하여 동기화합니다.
     * @param lang 수집할 언어 설정
     */
    @Transactional
    public void syncOneFestival(TourLanguage lang) {
        log.info("서울 축제/행사 정보 단건 sync 테스트 시작, language: {}", lang);

        TourApiResponseDto<TourApiEventItemDto> response = fetchFestivalsFromApi(lang);
        if (response != null && response.getResponse().getBody() != null) {
            log.info("syncOneFestival Response 갯수: {}", response.getResponse().getBody().getTotalCount());
        }

        if (response == null || response.getResponse().getBody().getItems() == null) {
            log.warn("조회된 데이터가 없습니다.");
            return;
        }

        List<TourApiEventItemDto> items = response.getResponse().getBody().getItems().getItem();
        if (!items.isEmpty()) {
            processItems(List.of(items.get(0)), lang);
            log.info("단건 동기화 완료: {}", items.get(0).getTitle());
        }
    }

    /**
     * API로부터 받은 목록 아이템들을 순회하며 DB에 저장 및 상세 정보를 갱신합니다.
     */
    private void processItems(List<TourApiEventItemDto> items, TourLanguage lang) {
        for (TourApiEventItemDto dto : items) {
            try {
                // 공통 부분 정보 처리(upsert)
                TourApiEvent event = eventRepository.findById(dto.getContentId()).orElseGet(
                        () -> TourApiEvent.builder().contentId(dto.getContentId()).build());
                updateEventEntity(event, dto);
                TourApiEvent managedEvent = eventRepository.save(event); // JPA 관리하도록 새로운 변수에 할당

                // 번역본 처리(upsert)
                TourApiEventTranslation translation = translationRepository.findByEventAndLanguage(managedEvent, lang).orElseGet(
                        () -> TourApiEventTranslation.builder().event(managedEvent).language(lang).build());

                translation.setTitle(dto.getTitle());
                translation.setAddress(dto.getAddr1() + (dto.getAddr2() != null ? " " + dto.getAddr2() : ""));

                // 상세 정보 동기화 (상세 설명, 축제 정보, 이미지 등 추가 API 호출)
                syncDetails(managedEvent, translation, lang);

                // 변경된 상세 정보 및 이미지 컬렉션 저장
                translationRepository.save(translation);
            } catch (Exception e) {
                log.error("[Sync Error] contentId: {} 처리 중 에러 발생. 다음 아이템으로 넘어갑니다. 에러: {}", 
                    dto.getContentId(), e.getMessage());
                // 루프가 멈추지 않도록 예외를 삼키고 다음 아이템 진행
            }
        }
    }

    /**
     * 콘텐츠 ID를 기반으로 공통/소개/이미지 상세 정보를 각각 호출하여 엔티티에 병합합니다.
     */
    private void syncDetails(TourApiEvent event, TourApiEventTranslation translation, TourLanguage lang) {
        Long contentId = event.getContentId();
        Long contentTypeId = event.getContentTypeId();
        log.info("[Detail Sync] 시작 - contentId: {}, contentTypeId: {}", contentId, contentTypeId);

        // 1. 공통 정보 조회 (개요, 홈페이지, 전화번호 명칭 등)
        TourApiDetailCommonItemDto commonDto = fetchDetailCommon(contentId, lang);
        if (commonDto != null) {
            log.info("[Detail Sync] 공통 정보 수집 완료 (overview 길이: {})", 
                commonDto.getOverview() != null ? commonDto.getOverview().length() : 0);
            translation.setOverview(commonDto.getOverview());
            translation.setHomepage(commonDto.getHomepage());
            translation.setTelName(commonDto.getTelName());
        }

        // 2. 소개 정보 조회 (축제 전용 상세 정보: 장소, 시간, 요금, 주최자 등)
        TourApiDetailIntroItemDto introDto = fetchDetailIntro(contentId, contentTypeId, lang);
        if (introDto != null) {
            log.info("[Detail Sync] 소개 정보 수집 완료 (eventPlace: {})", introDto.getEventPlace());
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

        // 3. 이미지 정보 조회 (갤러리 추가 이미지)
        List<TourApiDetailImageItemDto> imageDtos = fetchDetailImages(contentId, lang);
        if (imageDtos != null && !imageDtos.isEmpty()) {
            log.info("[Detail Sync] 이미지 정보 수집 완료 (이미지 개수: {})", imageDtos.size());
            // 기존 이미지 제거 후 최신 정보로 갱신 (orphanRemoval에 의해 DB 자동 반영)
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

            if (response != null && response.getResponse().getBody().getItems() != null && !response.getResponse().getBody().getItems().getItem().isEmpty()) {
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

            if (response != null && response.getResponse().getBody().getItems() != null && !response.getResponse().getBody().getItems().getItem().isEmpty()) {
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
//                            .queryParam("subImageYN", "Y")
                            .build())
                    .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiDetailImageItemDto>>() {
                    });

            if (response != null && response.getResponse().getBody().getItems() != null) {
                return response.getResponse().getBody().getItems().getItem();
            }
        } catch (Exception e) {
            log.error("[API Error] DetailImage 호출 실패: {}", e.getMessage());
            throw e;
        }
        return null;
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
                        .queryParam("eventStartDate", today)
                        .queryParam("lDongRegnCd", "11") // 서울 법정동 코드
                        .queryParam("numOfRows", 50)
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
    }

}
