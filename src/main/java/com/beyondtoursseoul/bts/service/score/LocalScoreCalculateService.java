package com.beyondtoursseoul.bts.service.score;

import com.beyondtoursseoul.bts.domain.DongLocalScore;
import com.beyondtoursseoul.bts.domain.DongPopulationRaw;
import com.beyondtoursseoul.bts.repository.DongLocalScoreRepository;
import com.beyondtoursseoul.bts.repository.DongPopulationRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalScoreCalculateService {

    private final DongPopulationRawRepository rawRepository;
    private final DongLocalScoreRepository scoreRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void calculateAndSave(LocalDate targetDate) {
        log.info("찐로컬 지수 계산 시작: {}", targetDate);
        LocalDate historyFrom = targetDate.minusDays(27);

        for (TimeSlot slot : TimeSlot.values()) {
            calculateForSlot(targetDate, historyFrom, slot);
        }

        log.info("찐로컬 지수 계산 완료: {}", targetDate);
    }

    private void calculateForSlot(LocalDate targetDate, LocalDate historyFrom, TimeSlot slot) {
        List<DongPopulationRaw> currentList = rawRepository.findByDateAndTimeSlot(targetDate, slot.getCode());
        if (currentList.isEmpty()) {
            log.warn("데이터 없음 — time_slot: {}, date: {}", slot.getCode(), targetDate);
            return;
        }

        // 과거 4주치 원시 데이터 (stability, repeat 계산용)
        List<DongPopulationRaw> historyList = rawRepository.findByTimeSlotAndDateBetween(
                slot.getCode(), historyFrom, targetDate);
        Map<String, List<DongPopulationRaw>> historyByDong = historyList.stream()
                .collect(Collectors.groupingBy(DongPopulationRaw::getDongCode));

        // 행정동별 원시 지표 계산
        Map<String, double[]> rawMetrics = new LinkedHashMap<>();
        for (DongPopulationRaw current : currentList) {
            String dongCode = current.getDongCode();
            List<DongPopulationRaw> history = historyByDong.getOrDefault(dongCode, List.of(current));

            double korean = current.getKoreanPop().doubleValue();
            double foreign = current.getForeignPop().doubleValue();
            double total = korean + foreign;

            double localRatio     = total > 0 ? korean / total : 0.5;
            double foreignPenalty = total > 0 ? foreign / total : 0.5;
            double stability      = calculateStability(history);
            double repeat         = calculateRepeat(history);

            // [0]=local_ratio, [1]=stability, [2]=repeat, [3]=foreign_penalty
            rawMetrics.put(dongCode, new double[]{localRatio, stability, repeat, foreignPenalty});
        }

        // MIN-MAX 정규화 (서울 전체 행정동 기준)
        double[] mins = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] maxs = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] m : rawMetrics.values()) {
            for (int i = 0; i < 4; i++) {
                if (m[i] < mins[i]) mins[i] = m[i];
                if (m[i] > maxs[i]) maxs[i] = m[i];
            }
        }

        // 기존 점수 삭제 후 재계산
        scoreRepository.deleteByDateAndTimeSlot(targetDate, slot.getCode());

        List<DongLocalScore> scores = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : rawMetrics.entrySet()) {
            double[] m = entry.getValue();
            double[] norm = new double[4];
            for (int i = 0; i < 4; i++) {
                double range = maxs[i] - mins[i];
                norm[i] = range > 1e-9 ? (m[i] - mins[i]) / range : 0.0;
            }

            double score = 0.3 * norm[0] + 0.3 * norm[1] + 0.3 * norm[2] - 0.1 * norm[3];
            score = Math.max(0.0, Math.min(1.0, score));

            scores.add(DongLocalScore.builder()
                    .dongCode(entry.getKey())
                    .date(targetDate)
                    .timeSlot(slot.getCode())
                    .score(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                    .breakdownJson(buildBreakdown(slot.getCode(), m, norm))
                    .build());
        }

        scoreRepository.saveAll(scores);
        log.info("time_slot {} 저장 완료: {}개 행정동", slot.getCode(), scores.size());
    }

    // stability = 1 - |평일평균 - 주말평균| / MAX(평일평균, 주말평균)
    private double calculateStability(List<DongPopulationRaw> history) {
        List<Double> weekday = new ArrayList<>();
        List<Double> weekend = new ArrayList<>();

        for (DongPopulationRaw row : history) {
            DayOfWeek dow = row.getDate().getDayOfWeek();
            double pop = row.getKoreanPop().doubleValue();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekend.add(pop);
            } else {
                weekday.add(pop);
            }
        }

        if (weekday.isEmpty() || weekend.isEmpty()) return 1.0;

        double wdAvg = weekday.stream().mapToDouble(d -> d).average().orElse(0);
        double weAvg = weekend.stream().mapToDouble(d -> d).average().orElse(0);
        double maxAvg = Math.max(wdAvg, weAvg);

        return maxAvg > 0 ? 1.0 - Math.abs(wdAvg - weAvg) / maxAvg : 1.0;
    }

    // repeat = 1 / (1 + 변동계수)  변동계수 = 표준편차 / 평균
    private double calculateRepeat(List<DongPopulationRaw> history) {
        if (history.size() < 2) return 1.0;

        double[] pops = history.stream()
                .mapToDouble(r -> r.getKoreanPop().doubleValue())
                .toArray();
        double mean = Arrays.stream(pops).average().orElse(0);
        if (mean < 1e-9) return 1.0;

        double variance = Arrays.stream(pops).map(p -> Math.pow(p - mean, 2)).average().orElse(0);
        double cv = Math.sqrt(variance) / mean;

        return 1.0 / (1.0 + cv);
    }

    private String buildBreakdown(String timeSlot, double[] raw, double[] norm) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("time_slot", timeSlot);
            map.put("local_ratio", round4(raw[0]));
            map.put("stability", round4(raw[1]));
            map.put("repeat", round4(raw[2]));
            map.put("foreign_penalty", round4(raw[3]));
            map.put("local_ratio_norm", round4(norm[0]));
            map.put("stability_norm", round4(norm[1]));
            map.put("repeat_norm", round4(norm[2]));
            map.put("foreign_penalty_norm", round4(norm[3]));
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
