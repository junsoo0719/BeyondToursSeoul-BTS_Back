package com.beyondtoursseoul.bts.service.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.TourEventDetailResponse;
import com.beyondtoursseoul.bts.dto.tour.TourEventSummaryResponse;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourQueryService {

    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;

    @Cacheable(value = "eventsPage", key = "#lang.name() + '_' + #pageable.pageNumber")
    public Page<TourEventSummaryResponse> getEventListPage(TourLanguage lang, Pageable pageable) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // DB에서 size만큼 가져옴
        Page<TourApiEvent> eventPage = eventRepository.findValidEventsPage(today, pageable);
        // 실제 데이터
        List<TourApiEvent> events = eventPage.getContent();

        if (events.isEmpty()) {
            return Page.empty(pageable); /// TODO: 무슨뜻
        }

        // 번역본 타겟
        List<TourLanguage> targetLanguages = lang == TourLanguage.KOR
                ? List.of(TourLanguage.KOR) : List.of(lang, TourLanguage.KOR);

        // 번역본 조회 (N + 1 방지 포함)
        Map<Long, Map<TourLanguage, TourApiEventTranslation>> translationMap = translationRepository
                .findByEventInAndLanguageIn(events, targetLanguages)
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEvent().getContentId(),
                        Collectors.toMap(TourApiEventTranslation::getLanguage, t -> t)
                ));

        // DB 추가 호출 안 하고 메모리 안에서 DTO 조합
        List<TourEventSummaryResponse> dtoList = events.stream()
                .map(event -> {
                    Map<TourLanguage, TourApiEventTranslation> eventTranslations = translationMap.getOrDefault(event.getContentId(), Map.of());

                    TourApiEventTranslation translation = eventTranslations.get(lang);
                    if (translation == null) {
                        translation = eventTranslations.get(TourLanguage.KOR);
                    }
                    if (translation == null) {
                        return null;
                    }
                    return new TourEventSummaryResponse(event, translation);
                }).filter(Objects::nonNull).collect(Collectors.toList());

        // 프론트엔드에서 페이징 처리 할 수 있도록 PageImpl로 포장해서 반환
        return new PageImpl<>(dtoList, pageable, eventPage.getTotalElements());
    }


    /**
     * 특정 언어에 맞는 문화행사 리스트를 조회합니다.(종료 날짜가 어제 이후)
     */
    // 'events'라는 캐시에 저장, 'lang' 파라미터 값을 키로 사용
    @Cacheable(value = "events", key = "#lang")
    public List<TourEventSummaryResponse> getEventList(TourLanguage lang) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1. 종료되지 않은 행사 목록을 먼저 조회 (쿼리 1번)
        List<TourApiEvent> events = eventRepository.findValidEvents(today);
        if (events.isEmpty()) {
            return List.of();
        }

        // 2. 필요한 언어(요청 언어 + 대체용 국문)를 지정
        List<TourLanguage> targetLanguages = lang == TourLanguage.KOR
                ? List.of(TourLanguage.KOR)
                : List.of(lang, TourLanguage.KOR);

        // 3. 행사 ID와 언어에 맞는 번역본들을 단 1번의 IN 쿼리로 모두 가져옴 (쿼리 1번)
        Map<Long, Map<TourLanguage, TourApiEventTranslation>> translationMap = translationRepository
                .findByEventInAndLanguageIn(events, targetLanguages)
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEvent().getContentId(),
                        Collectors.toMap(TourApiEventTranslation::getLanguage, t -> t)
                ));

        // 4. DB 접근 없이 메모리에서 데이터를 조합하여 DTO 생성
        return events.stream()
                .map(event -> {
                    Map<TourLanguage, TourApiEventTranslation> eventTranslations = translationMap.getOrDefault(event.getContentId(), Map.of());

                    // 요청 언어 번역본을 먼저 찾고, 없으면 국문 번역본으로 대체
                    TourApiEventTranslation translation = eventTranslations.get(lang);
                    if (translation == null) {
                        translation = eventTranslations.get(TourLanguage.KOR);
                    }

                    // 국문 번역본조차 없으면 리스트에서 제외
                    if (translation == null) return null;

                    return new TourEventSummaryResponse(event, translation);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 특정 언어에 맞는 문화행사 상세 정보를 조회합니다.
     */
    public TourEventDetailResponse getEventDetail(Long contentId, TourLanguage lang) {
        TourApiEvent event = eventRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행사입니다. ID: " + contentId));

        // 요청한 언어의 번역본 조회, 없으면 국문(KOR)으로 대체
        TourApiEventTranslation translation = translationRepository
                .findByEventAndLanguage(event, lang)
                .orElseGet(() -> translationRepository.findByEventAndLanguage(event, TourLanguage.KOR)
                        .orElseThrow(() -> new IllegalStateException("해당 행사의 국문 데이터가 존재하지 않습니다.")));

        return new TourEventDetailResponse(event, translation);
    }
}
