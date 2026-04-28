package com.beyondtoursseoul.bts.service.score;

import com.beyondtoursseoul.bts.dto.ForeignResidentApiResponseDto;
import com.beyondtoursseoul.bts.dto.LocalResidentApiResponseDto;
import com.beyondtoursseoul.bts.service.ForeignResidentApiService;
import com.beyondtoursseoul.bts.service.LocalResidentApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopulationCollectService {

    private final LocalResidentApiService localResidentApiService;
    private final ForeignResidentApiService foreignResidentApiService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void collect(LocalDate date) {
        log.info("생활인구 수집 시작: {}", date);

        List<LocalResidentApiResponseDto.Row> koreanRows = localResidentApiService.fetchByDate(date);
        List<ForeignResidentApiResponseDto.Row> foreignRows = foreignResidentApiService.fetchByDate(date);

        Map<String, Map<TimeSlot, List<Double>>> koreanByDong = groupByDongAndSlot(koreanRows);
        Map<String, Map<TimeSlot, List<Double>>> foreignByDong = groupByDongAndSlot(foreignRows);

        Set<String> allDongs = new HashSet<>(koreanByDong.keySet());
        allDongs.addAll(foreignByDong.keySet());

        List<Object[]> batchArgs = new ArrayList<>();
        for (String dongCode : allDongs) {
            for (TimeSlot slot : TimeSlot.values()) {
                double koreanAvg = average(koreanByDong.getOrDefault(dongCode, Map.of())
                        .getOrDefault(slot, List.of()));
                double foreignAvg = average(foreignByDong.getOrDefault(dongCode, Map.of())
                        .getOrDefault(slot, List.of()));

                batchArgs.add(new Object[]{
                        dongCode,
                        date,
                        slot.getCode(),
                        BigDecimal.valueOf(koreanAvg).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(foreignAvg).setScale(4, RoundingMode.HALF_UP)
                });
            }
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO dong_population_raw (dong_code, date, time_slot, korean_pop, foreign_pop) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (dong_code, date, time_slot) DO UPDATE SET " +
                "korean_pop = EXCLUDED.korean_pop, foreign_pop = EXCLUDED.foreign_pop",
                batchArgs
        );

        log.info("생활인구 저장 완료: {} ({} 행정동 × 5 슬롯)", date, allDongs.size());
    }

    private <T> Map<String, Map<TimeSlot, List<Double>>> groupByDongAndSlot(List<T> rows) {
        Map<String, Map<TimeSlot, List<Double>>> result = new HashMap<>();
        for (T row : rows) {
            String dongCode;
            String timeZone;
            String totalPop;

            if (row instanceof LocalResidentApiResponseDto.Row r) {
                dongCode = r.getDongCode();
                timeZone = r.getTimeZone();
                totalPop = r.getTotalPopulation();
            } else if (row instanceof ForeignResidentApiResponseDto.Row r) {
                dongCode = r.getDongCode();
                timeZone = r.getTimeZone();
                totalPop = r.getTotalPopulation();
            } else {
                continue;
            }

            if (dongCode == null || timeZone == null) continue;

            int hour;
            try {
                hour = Integer.parseInt(timeZone);
            } catch (NumberFormatException e) {
                continue;
            }

            TimeSlot.fromHour(hour).ifPresent(slot ->
                    result.computeIfAbsent(dongCode, k -> new EnumMap<>(TimeSlot.class))
                          .computeIfAbsent(slot, k -> new ArrayList<>())
                          .add(parseDouble(totalPop))
            );
        }
        return result;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).average().orElse(0.0);
    }
}
