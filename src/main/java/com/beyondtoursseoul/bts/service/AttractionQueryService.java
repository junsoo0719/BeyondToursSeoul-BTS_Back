package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.TourCategory;
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
import java.util.ArrayList;
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

    public List<AttractionSummaryResponse> getList(LocalDate date, String timeSlot,
                                                    BigDecimal minScore, BigDecimal maxScore,
                                                    String lang) {
        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        List<Object[]> rows = attractionRepository.findWithLocalScoresForList(
                effectiveDate, timeSlot, minScore, maxScore);

        Map<String, TourCategory> categories = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));

        List<Long> attractionIds = rows.stream()
                .map(r -> ((Attraction) r[0]).getId())
                .collect(Collectors.toList());

        Map<Long, AttractionTranslation> translationMap = isKorean(lang) || attractionIds.isEmpty()
                ? Map.of()
                : translationRepository.findByIdAttractionIdInAndIdLang(attractionIds, lang)
                        .stream()
                        .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t));

        List<AttractionSummaryResponse> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Attraction a = (Attraction) row[0];
            AttractionLocalScore s = (AttractionLocalScore) row[1];
            out.add(new AttractionSummaryResponse(
                    a,
                    s,
                    resolveCategoryName(categories, a.getCat1(), lang),
                    resolveCategoryName(categories, a.getCat2(), lang),
                    resolveCategoryName(categories, a.getCat3(), lang),
                    translationMap.get(a.getId())
            ));
        }
        return out;
    }

    private String resolveCategoryName(Map<String, TourCategory> categories, String code, String lang) {
        if (code == null) return "미분류";
        TourCategory category = categories.get(code);
        if (category == null) return "미분류";
        return category.getLocalizedName(lang);
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

        Map<String, TourCategory> categories = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));

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
                resolveCategoryName(categories, attraction.getCat1(), lang),
                resolveCategoryName(categories, attraction.getCat2(), lang),
                resolveCategoryName(categories, attraction.getCat3(), lang),
                scores,
                translation
        );
    }
}
