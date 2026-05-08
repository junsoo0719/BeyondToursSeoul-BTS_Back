package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.dto.attraction.AttractionDetailResponse;
import com.beyondtoursseoul.bts.dto.attraction.AttractionSummaryResponse;
import com.beyondtoursseoul.bts.repository.AttractionLocalScoreRepository;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.AttractionTranslationRepository;
import com.beyondtoursseoul.bts.repository.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttractionQueryService {

    private final AttractionRepository attractionRepository;
    private final AttractionLocalScoreRepository scoreRepository;
    private final TourCategoryRepository categoryRepository;
    private final AttractionApiService attractionApiService;
    private final AttractionTranslationRepository translationRepository;

    public List<AttractionSummaryResponse> getList(LocalDate date, String timeSlot, String lang) {
        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        Map<Long, AttractionLocalScore> scoreMap = scoreRepository
                .findByIdDateAndIdTimeSlot(effectiveDate, timeSlot)
                .stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), s -> s));

        Map<String, String> categoryNames = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(c -> c.getCode(), c -> c.getName()));

        List<Attraction> attractions = attractionRepository.findAll().stream()
                .filter(a -> scoreMap.containsKey(a.getId()))
                .collect(Collectors.toList());

        Map<Long, AttractionTranslation> translationMap = isKorean(lang) ? Map.of()
                : translationRepository.findByIdAttractionIdInAndIdLang(
                        attractions.stream().map(Attraction::getId).collect(Collectors.toList()), lang)
                  .stream()
                  .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t));

        return attractions.stream()
                .map(a -> new AttractionSummaryResponse(
                        a,
                        scoreMap.get(a.getId()),
                        resolveCategoryName(categoryNames, a.getCat1()),
                        resolveCategoryName(categoryNames, a.getCat2()),
                        resolveCategoryName(categoryNames, a.getCat3()),
                        translationMap.get(a.getId())
                ))
                .sorted(Comparator.comparing(AttractionSummaryResponse::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private String resolveCategoryName(Map<String, String> categoryNames, String code) {
        if (code == null) return "미분류";
        return categoryNames.getOrDefault(code, "미분류");
    }

    private boolean isKorean(String lang) {
        return lang == null || lang.isBlank() || lang.toLowerCase().startsWith("ko");
    }

    @Transactional
    public AttractionDetailResponse getDetail(Long id, String lang) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("관광지를 찾을 수 없습니다: " + id));

        if (!attraction.isDetailFetched() && attraction.getExternalId() != null) {
            AttractionApiService.CommonDetail common = attractionApiService.fetchCommonDetail(attraction.getExternalId());
            String operatingHours = attractionApiService.fetchOperatingHours(
                    attraction.getExternalId(), attraction.getCategory());
            attraction.updateDetail(common.overview(), operatingHours, common.tel());
        }

        Map<String, String> categoryNames = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(c -> c.getCode(), c -> c.getName()));

        LocalDate latestDate = scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));
        Map<String, BigDecimal> scores = scoreRepository.findByIdAttractionIdAndIdDate(id, latestDate)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getId().getTimeSlot(),
                        AttractionLocalScore::getScore
                ));

        AttractionTranslation translation = isKorean(lang) ? null
                : translationRepository.findByIdAttractionIdAndIdLang(id, lang).orElse(null);

        return new AttractionDetailResponse(
                attraction,
                resolveCategoryName(categoryNames, attraction.getCat1()),
                resolveCategoryName(categoryNames, attraction.getCat2()),
                resolveCategoryName(categoryNames, attraction.getCat3()),
                scores,
                translation
        );
    }
}
