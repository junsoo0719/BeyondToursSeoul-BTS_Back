package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.dto.attraction.AttractionDetailResponse;
import com.beyondtoursseoul.bts.dto.attraction.AttractionSummaryResponse;
import com.beyondtoursseoul.bts.repository.AttractionLocalScoreRepository;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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

    public List<AttractionSummaryResponse> getList(LocalDate date, String timeSlot) {
        LocalDate effectiveDate = date != null ? date
                : scoreRepository.findLatestDate().orElse(LocalDate.now().minusDays(1));

        Map<Long, AttractionLocalScore> scoreMap = scoreRepository
                .findByIdDateAndIdTimeSlot(effectiveDate, timeSlot)
                .stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), s -> s));

        Map<String, String> categoryNames = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(c -> c.getCode(), c -> c.getName()));

        return attractionRepository.findAll().stream()
                .filter(a -> scoreMap.containsKey(a.getId()))
                .map(a -> new AttractionSummaryResponse(
                        a,
                        scoreMap.get(a.getId()),
                        categoryNames.getOrDefault(a.getCat1(), a.getCat1()),
                        categoryNames.getOrDefault(a.getCat2(), a.getCat2()),
                        categoryNames.getOrDefault(a.getCat3(), a.getCat3())
                ))
                .sorted(Comparator.comparing(AttractionSummaryResponse::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public AttractionDetailResponse getDetail(Long id) {
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

        return new AttractionDetailResponse(
                attraction,
                categoryNames.getOrDefault(attraction.getCat1(), attraction.getCat1()),
                categoryNames.getOrDefault(attraction.getCat2(), attraction.getCat2()),
                categoryNames.getOrDefault(attraction.getCat3(), attraction.getCat3()),
                scores
        );
    }
}
