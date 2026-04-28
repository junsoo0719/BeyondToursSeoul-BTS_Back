package com.beyondtoursseoul.bts.service.score;

import java.util.Optional;

public enum TimeSlot {

    MORNING("morning", 6),
    LUNCH("lunch", 11),
    AFTERNOON("afternoon", 14),
    EVENING("evening", 18),
    NIGHT("night", 22);

    private final String code;
    private final int startHour; // dong_local_score.hour 에 저장되는 값

    TimeSlot(String code, int startHour) {
        this.code = code;
        this.startHour = startHour;
    }

    public String getCode() { return code; }
    public int getStartHour() { return startHour; }

    // 06~10 → MORNING, 11~13 → LUNCH, 14~17 → AFTERNOON, 18~21 → EVENING
    // 22,23,00,01,02 → NIGHT (같은 날짜 기준으로 묶음)
    // 03,04,05 → 미사용
    public static Optional<TimeSlot> fromHour(int hour) {
        if (hour >= 6 && hour < 11)  return Optional.of(MORNING);
        if (hour >= 11 && hour < 14) return Optional.of(LUNCH);
        if (hour >= 14 && hour < 18) return Optional.of(AFTERNOON);
        if (hour >= 18 && hour < 22) return Optional.of(EVENING);
        if (hour >= 22 || hour < 3)  return Optional.of(NIGHT);
        return Optional.empty();
    }
}
