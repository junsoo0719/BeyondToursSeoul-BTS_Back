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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final AttractionTranslationRepository translationRepository;

    @Lazy
    @Autowired
    private AttractionQueryService self;


    // 캐시 키에 #category 추가
    @Cacheable(value = "attractionsPage", key = "{#category, #date, #timeSlot, #minScore, #maxScore, #lang, #pageable.pageNumber}")
    public Page<AttractionSummaryResponse> getListPage(String category, LocalDate date, String timeSlot,
                                                       BigDecimal minScore, BigDecimal maxScore,
                                                       String lang, Pageable pageable) {

        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        // Null 파라미터로 인한 Postgres bytea 에러를 방지하기 위해 boolean 플래그와 검색 키워드를 Java에서 생성
        boolean hasCategory = category != null && !category.isBlank();
        String categoryKeyword = hasCategory ? "%" + category + "%" : "";

        // 1. DB에서 조건에 맞는 딱 10개의 관광지 데이터와 점수를 조인해서 가져옵니다 (초고속 페이징)
        Page<Object[]> pageResult = attractionRepository.findWithLocalScoresPage(
                effectiveDate, timeSlot, minScore, maxScore, hasCategory, categoryKeyword, pageable);

        // 2. 결과가 없으면 빈 페이지 반환
        if (pageResult.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, TourCategory> categories = self.getCategoryMap();

        // 3. 가져온 10개의 ID만 추출
        List<Long> attractionIds = pageResult.stream()
                .map(r -> ((Attraction) r[0]).getId())
                .collect(Collectors.toList());

        // 4. 딱 10개의 번역 데이터만 IN 절로 싹쓸이 (N+1 방어 로직 완벽 유지)
        Map<Long, AttractionTranslation> translationMap = isKorean(lang)
                ? Map.of()
                : translationRepository.findByIdAttractionIdInAndIdLang(attractionIds, lang)
                  .stream()
                  .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t));

        // 5. DTO 조립
        List<AttractionSummaryResponse> out = new ArrayList<>(pageResult.getNumberOfElements());
        for (Object[] row : pageResult) {
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

        // 6. 프론트엔드가 페이징 처리를 할 수 있도록 PageImpl 포장 반환
        return new PageImpl<>(out, pageable, pageResult.getTotalElements());
    }

    // 특별히 key를 적지 않으면 메서드 파라미터 전체를 조합해 자동으로 고유 키를 생성해줌
    @Cacheable(value = "attractions")
    public List<AttractionSummaryResponse> getList(String category, LocalDate date, String timeSlot,
                                                   BigDecimal minScore, BigDecimal maxScore,
                                                   String lang) {
        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        boolean hasCategory = category != null && !category.isBlank();
        String categoryKeyword = hasCategory ? "%" + category + "%" : "";

        List<Object[]> rows = attractionRepository.findWithLocalScoresForList(
                effectiveDate, timeSlot, minScore, maxScore, hasCategory, categoryKeyword);

        Map<String, TourCategory> categories = self.getCategoryMap();

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

    @Cacheable("tourCategories")
    public Map<String, TourCategory> getCategoryMap() {
        return categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));
    }

    private boolean isInScoreRange(BigDecimal score, BigDecimal min, BigDecimal max) {
        if (score == null) {
            return min == null && max == null;
        }
        if (min != null && score.compareTo(min) < 0) {
            return false;
        }
        if (max != null && score.compareTo(max) > 0) {
            return false;
        }
        return true;
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

    @Cacheable(value = "attractionDetail", key = "{#id, #lang}")
    @Transactional(readOnly = true)
    public AttractionDetailResponse getDetail(Long id, String lang) {
        Attraction attraction = attractionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("관광지를 찾을 수 없습니다: " + id));

        Map<String, TourCategory> categories = self.getCategoryMap();

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
