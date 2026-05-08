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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttractionScoreServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock TransactionTemplate transactionTemplate;
    @Mock AttractionLocalScoreRepository repository;

    @InjectMocks AttractionScoreService service;

    @Test
    void 신규_날짜면_전체_시간대_5회_INSERT한다() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(repository.findLatestDate()).thenReturn(Optional.empty());
        // advisory lock/unlock은 인프라 역할이므로 lenient — non-lenient stub이 없어야 unlock 호출 시 PotentialStubbingProblem 방지
        lenient().when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), any()))
                .thenReturn(true);
        lenient().when(jdbcTemplate.queryForObject(contains("pg_advisory_unlock"), eq(Boolean.class), any()))
                .thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        // DELETE(2 args)와 INSERT(5 args) 모두 lenient — non-lenient stub이 있으면 arg 불일치 시 PotentialStubbingProblem 발생
        lenient().when(jdbcTemplate.update(contains("DELETE"), any(), any())).thenReturn(0);
        lenient().when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any())).thenReturn(100);

        service.calculateAndSave(date);

        verify(jdbcTemplate, times(TimeSlot.values().length))
                .update(any(String.class), any(), any(), any(), any(), any());
    }

    @Test
    void 이미_계산된_날짜면_INSERT하지_않는다() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(repository.findLatestDate()).thenReturn(Optional.of(date));

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
