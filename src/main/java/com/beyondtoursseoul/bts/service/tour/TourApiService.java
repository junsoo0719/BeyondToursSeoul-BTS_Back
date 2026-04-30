package com.beyondtoursseoul.bts.service.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.TourApiEventItemDto;
import com.beyondtoursseoul.bts.dto.tour.TourApiResponseDto;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class TourApiService {

    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;
    private final RestClient restClient;

    @Value("${public.data.api.key}")
    private String tourApiKey;

    private static final String PUBLIC_DATA_API_URL =
            "https://apis.data.go.kr/B551011/";

    public TourApiService(TourApiEventRepository eventRepository, TourApiEventTranslationRepository translationRepository) {
        this.eventRepository = eventRepository;
        this.translationRepository = translationRepository;
        this.restClient = RestClient.builder().baseUrl(PUBLIC_DATA_API_URL).build();
    }


    @Transactional
    public void syncFestivals(TourLanguage lang) {
        log.info("서울 축제/행사 정보 sync, language: {}", lang);

        TourApiResponseDto<TourApiEventItemDto> response = fetchFestivalsFromApi(lang);

        if (response == null || response.getResponse().getBody().getItems() == null) {
            log.warn("Tour Api 조회 결과, 데이터가 비어있습니다. language: {}", lang);
            return;
        }

        // 응답에서 아이템리스트 뽑아오기
        List<TourApiEventItemDto> items = response.getResponse().getBody().getItems().getItem();

        for (TourApiEventItemDto dto : items) {
            // 공통 부분 정보 처리(upsert)
            TourApiEvent event = eventRepository.findById(dto.getContentId()).orElseGet(
                    () -> TourApiEvent.builder().contentId(dto.getContentId()).build());
            updateEventEntity(event, dto);
            eventRepository.save(event);

            // 번역본
            TourApiEventTranslation translation = translationRepository.findByEventAndLanguage(event, lang).orElseGet(
                    () -> TourApiEventTranslation.builder().event(event).language(lang).build());

            translation.setTitle(dto.getTitle());
            translation.setAddress(dto.getAddr1() + (dto.getAddr2() != null ? " " + dto.getAddr2() : ""));
            /// TODO: 추후 overview 채워야함
            translationRepository.save(translation);
        }
        log.info("Successfully synced {} items for language: {}", items.size(), lang);

    }

    /**
     * API 원본 데이터를 DTO로 반환 (테스트용)
     */
    public TourApiResponseDto<TourApiEventItemDto> getRawFestivals(TourLanguage lang) {
        return fetchFestivalsFromApi(lang);
    }

    private TourApiResponseDto<TourApiEventItemDto> fetchFestivalsFromApi(TourLanguage lang) {
        String url = lang.getServiceName() + "/searchFestival2";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .queryParam("serviceKey", tourApiKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "BeyondToursSeoul")
                        .queryParam("_type", "json")
                        .queryParam("eventStartDate", today) // [필수 수정] 필수
                        .queryParam("lDongRegnCd", "11")      // [수정] 서울 법정동
//                        .queryParam("areaCode", "11")
//                        .queryParam("numOfRows", 50)
                        .build())
                .retrieve().body(new ParameterizedTypeReference<TourApiResponseDto<TourApiEventItemDto>>() {
                });
    }

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
        event.setAreaCode(dto.getAreaCode());
        event.setSigunguCode(dto.getSigunguCode());
        event.setModifiedTime(dto.getModifiedTime());
        event.setLastSyncTime(LocalDateTime.now());
    }

}