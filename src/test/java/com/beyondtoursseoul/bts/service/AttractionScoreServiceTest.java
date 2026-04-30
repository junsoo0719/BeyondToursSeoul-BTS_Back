package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.repository.AttractionLocalScoreRepository;
import com.beyondtoursseoul.bts.service.score.AttractionScoreService;
import com.beyondtoursseoul.bts.service.score.TimeSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttractionScoreServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock AttractionLocalScoreRepository repository;

    @InjectMocks AttractionScoreService service;

    @Test
    void 신규_날짜면_전체_시간대_5회_INSERT한다() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(repository.existsByIdDate(date)).thenReturn(false);
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any())).thenReturn(100);

        service.calculateAndSave(date);

        verify(jdbcTemplate, times(TimeSlot.values().length)).update(any(String.class), any(), any(), any(), any(), any());
    }

    @Test
    void 이미_계산된_날짜면_INSERT하지_않는다() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(repository.existsByIdDate(date)).thenReturn(true);

        service.calculateAndSave(date);

        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any());
    }

    @Test
    void 카테고리별_대표_시간대_매핑이_올바르다() {
        assertThat(AttractionScoreService.primaryTimeSlot("39")).isEqualTo(TimeSlot.EVENING);   // 음식점
        assertThat(AttractionScoreService.primaryTimeSlot("15")).isEqualTo(TimeSlot.EVENING);   // 행사/공연
        assertThat(AttractionScoreService.primaryTimeSlot("28")).isEqualTo(TimeSlot.MORNING);   // 레포츠
        assertThat(AttractionScoreService.primaryTimeSlot("32")).isEqualTo(TimeSlot.MORNING);   // 숙박
        assertThat(AttractionScoreService.primaryTimeSlot("12")).isEqualTo(TimeSlot.AFTERNOON); // 관광지
        assertThat(AttractionScoreService.primaryTimeSlot("14")).isEqualTo(TimeSlot.AFTERNOON); // 문화시설
        assertThat(AttractionScoreService.primaryTimeSlot("38")).isEqualTo(TimeSlot.AFTERNOON); // 쇼핑
        assertThat(AttractionScoreService.primaryTimeSlot(null)).isEqualTo(TimeSlot.AFTERNOON); // null
    }
}
