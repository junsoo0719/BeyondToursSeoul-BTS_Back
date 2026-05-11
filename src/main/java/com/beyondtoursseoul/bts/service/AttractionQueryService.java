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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


    // 캐시 키에 #category 추가
    @Cacheable(value = "attractionsPage", key = "{#category, #date, #timeSlot, #minScore, #maxScore, #lang, #pageable.pageNumber}")
    public Page<AttractionSummaryResponse> getListPage(String category, LocalDate date, String timeSlot,
                                                       BigDecimal minScore, BigDecimal maxScore,
                                                       String lang, Pageable pageable) {

        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        // 1. 점수 리스트 쫙 가져오기 (가벼움)
        List<AttractionLocalScore> scoreList = scoreRepository.findByIdDateAndIdTimeSlot(effectiveDate, timeSlot);

        // 2. 카테고리가 있다면 해당 ID 목록만 따로 가져오기
        boolean hasCategory = category != null && !category.isBlank();
        Set<Long> categoryIdSet = null;
        if (hasCategory) {
            String categoryKeyword = "%" + category + "%";
            categoryIdSet = new HashSet<>(attractionRepository.findIdsByCategoryNameAnyLang(categoryKeyword));
        }
        final Set<Long> finalCategoryIds = categoryIdSet;

        // 3. 필터링 및 정렬 (Java 메모리에서 초고속 처리, DB의 무거운 ORDER BY 부하 완벽 해결)
        List<AttractionLocalScore> filteredScores = scoreList.stream()
                .filter(s -> isInScoreRange(s.getScore(), minScore, maxScore))
                .filter(s -> finalCategoryIds == null || finalCategoryIds.contains(s.getId().getAttractionId()))
                .sorted(Comparator.comparing(AttractionLocalScore::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 4. 페이지네이션 (딱 10개만 자르기)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredScores.size());

        if (start >= filteredScores.size()) {
            return Page.empty(pageable);
        }

        // 5. 잘라낸 10개의 가벼운 점수 객체
        List<AttractionLocalScore> pageScores = filteredScores.subList(start, end);
        List<Long> pageIds = pageScores.stream()
                .map(s -> s.getId().getAttractionId())
                .collect(Collectors.toList());

        // 6. DB에서 무거운 관광지 데이터는 딱 10개만 조회! (OOM 방어)
        List<Attraction> attractions = attractionRepository.findAllById(pageIds);

        // 7. 카테고리 정보, 번역 정보 매핑
        Map<String, TourCategory> categories = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));

        Map<Long, AttractionTranslation> translationMap = isKorean(lang)
                ? Map.of()
                : translationRepository.findByIdAttractionIdInAndIdLang(pageIds, lang)
                  .stream()
                  .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t));

        Map<Long, AttractionLocalScore> scoreMap = pageScores.stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), s -> s));

        // DB IN 절 결과는 순서가 보장되지 않으므로, pageIds 순서대로 재배치
        Map<Long, Attraction> attractionMap = attractions.stream()
                .collect(Collectors.toMap(Attraction::getId, a -> a));

        List<AttractionSummaryResponse> dtoList = pageIds.stream()
                .filter(attractionMap::containsKey) // 안전장치
                .map(id -> {
                    Attraction a = attractionMap.get(id);
                    return new AttractionSummaryResponse(
                            a,
                            scoreMap.get(id),
                            resolveCategoryName(categories, a.getCat1(), lang),
                            resolveCategoryName(categories, a.getCat2(), lang),
                            resolveCategoryName(categories, a.getCat3(), lang),
                            translationMap.get(id)
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, filteredScores.size());
    }

    // 특별히 key를 적지 않으면 메서드 파라미터 전체를 조합해 자동으로 고유 키를 생성해줌
    @Cacheable(value = "attractions")
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
