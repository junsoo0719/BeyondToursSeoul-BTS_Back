package com.beyondtoursseoul.bts.service.tour;

import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.tour.TourEventDetailResponse;
import com.beyondtoursseoul.bts.dto.tour.TourEventSummaryResponse;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourQueryService {

    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;

    /**
     * 특정 언어에 맞는 문화행사 리스트를 조회합니다.(종료 날짜가 어제 이후)
     */
    public List<TourEventSummaryResponse> getEventList(TourLanguage lang) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return eventRepository.findValidEvents(today).stream()
                .map(event -> {
                    // 요청한 언어의 번역본 조회, 없으면 국문(KOR)으로 대체
                    TourApiEventTranslation translation = translationRepository
                            .findByEventAndLanguage(event, lang)
                            .orElseGet(() -> translationRepository.findByEventAndLanguage(event, TourLanguage.KOR)
                                    .orElse(null));
                    
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
