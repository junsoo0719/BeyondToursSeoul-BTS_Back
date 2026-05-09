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


    // 👇 [신규 추가] 페이지 번호를 포함하여 완벽하게 파라미터별로 독립된 캐시 생성
    @Cacheable(value = "attractionsPage", key = "{#date, #timeSlot, #minScore, #maxScore, #lang, #pageable.pageNumber}")
    public Page<AttractionSummaryResponse> getListPage(LocalDate date, String timeSlot,
                                                       BigDecimal minScore, BigDecimal maxScore,
                                                       String lang, Pageable pageable) {

        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        // 점수 리스트
        List<AttractionLocalScore> scoreList = scoreRepository
                .findByIdDateAndIdTimeSlot(effectiveDate, timeSlot);

        // 점수 범위로 필터링 후, 내림차순 정렬하여 전체 '관광지 ID 풀(Pool)' 만들기
        List<AttractionLocalScore> filteredScores = scoreList.stream()
                .filter(s -> isInScoreRange(s.getScore(), minScore, maxScore))
                .sorted(Comparator.comparing(AttractionLocalScore::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 페이지네이션 정보에 맞게 딱 10개(요청 size)의 데이터만 자름
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredScores.size());

        if (start >= filteredScores.size()) {
            return Page.empty(pageable); // 범위를 벗어나면 빈 페이지 반환
        }

        // 10개의 점수 객체와 ID 리스트만 추출
        List<AttractionLocalScore> pageScores = filteredScores.subList(start, end);
        List<Long> pageIds = pageScores.stream()
                .map(s -> s.getId().getAttractionId())
                .collect(Collectors.toList());

        // DB에는 딱 10개의 ID만 던져서 무거운 관광지 객체 10개만 가져옴
        List<Attraction> attractions = attractionRepository.findAllById(pageIds);

        // 5. 카테고리 정보 및 번역 정보 매핑 (번역 정보도 딱 10개만 IN 절로 싹쓸이!)
        Map<String, TourCategory> categories = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));

        Map<Long, AttractionTranslation> translationMap = isKorean(lang) ? Map.of()
                : translationRepository.findByIdAttractionIdInAndIdLang(pageIds, lang)
                  .stream()
                  .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t));

        Map<Long, AttractionLocalScore> scoreMap = pageScores.stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), s -> s));

        // DB에서 IN 절로 가져오면 순서가 뒤섞일 수 있으므로, 재정렬을 위해 Map 구조 활용
        Map<Long, Attraction> attractionMap = attractions.stream()
                .collect(Collectors.toMap(Attraction::getId, a -> a));

        // 점수순으로 정렬되어 있는 pageIds 순서 그대로 DTO 조립
        List<AttractionSummaryResponse> dtoList = pageIds.stream()
                .filter(attractionMap::containsKey) // 안전 장치
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

        Map<Long, AttractionLocalScore> scoreMap = scoreRepository
                .findByIdDateAndIdTimeSlot(effectiveDate, timeSlot)
                .stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), s -> s));

        Map<String, TourCategory> categories = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TourCategory::getCode, c -> c));

        List<Attraction> attractions = attractionRepository.findAll().stream()
                .filter(a -> scoreMap.containsKey(a.getId()))
                .filter(a -> isInScoreRange(scoreMap.get(a.getId()).getScore(), minScore, maxScore))
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
                        resolveCategoryName(categories, a.getCat1(), lang),
                        resolveCategoryName(categories, a.getCat2(), lang),
                        resolveCategoryName(categories, a.getCat3(), lang),
                        translationMap.get(a.getId())
                ))
                .sorted(Comparator.comparing(AttractionSummaryResponse::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private boolean isInScoreRange(BigDecimal score, BigDecimal min, BigDecimal max) {
        if (score == null) return min == null && max == null;
        if (min != null && score.compareTo(min) < 0) return false;
        if (max != null && score.compareTo(max) > 0) return false;
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
