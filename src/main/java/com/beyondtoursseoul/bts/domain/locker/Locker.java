package com.beyondtoursseoul.bts.domain.locker;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lockers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 서울시 API의 물품보관함 고유 아이디 (lckrId)
    @Column(name = "lckr_id", unique = true, nullable = false, length = 50)
    private String lckrId;

    // 위도 (lat)
    @Column(nullable = false)
    private Double latitude;

    // 경도 (lot)
    @Column(nullable = false)
    private Double longitude;

    // 전체 보관함 개수 (lckrCnt)
    @Column(name = "total_cnt")
    private Integer totalCnt;

    // 평일 운영 시작 시각 (wkdayOperBgngTm)
    @Column(name = "weekday_start_time", length = 10)
    private String weekdayStartTime;

    // 평일 운영 종료 시각 (wkdayOperEndTm)
    @Column(name = "weekday_end_time", length = 10)
    private String weekdayEndTime;

    // 주말/공휴일 운영 시작 시각 (satOperBgngTm)
    @Column(name = "weekend_start_time", length = 10)
    private String weekendStartTime;

    // 주말/공휴일 운영 종료 시각 (satOperEndTm)
    @Column(name = "weekend_end_time", length = 10)
    private String weekendEndTime;

    // 추가 요금 단위 시간(분) (addCrgUnitHr)
    @Column(name = "add_charge_unit")
    private Integer addChargeUnit;

    // 양방향 매핑 (Locker가 삭제되면 Translation도 함께 삭제되도록 cascade 설정)
    @OneToMany(mappedBy = "locker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LockerTranslation> translations = new ArrayList<>();

    @Builder
    public Locker(String lckrId, Double latitude, Double longitude, Integer totalCnt,
                  String weekdayStartTime, String weekdayEndTime,
                  String weekendStartTime, String weekendEndTime, Integer addChargeUnit) {
        this.lckrId = lckrId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalCnt = totalCnt;
        this.weekdayStartTime = weekdayStartTime;
        this.weekdayEndTime = weekdayEndTime;
        this.weekendStartTime = weekendStartTime;
        this.weekendEndTime = weekendEndTime;
        this.addChargeUnit = addChargeUnit;
    }
    
    // 추가: 엔티티 정보를 최신으로 덮어쓰는 메서드
    public void update(Double latitude, Double longitude, Integer totalCnt,
                       String weekdayStartTime, String weekdayEndTime,
                       String weekendStartTime, String weekendEndTime, Integer addChargeUnit) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalCnt = totalCnt;
        this.weekdayStartTime = weekdayStartTime;
        this.weekdayEndTime = weekdayEndTime;
        this.weekendStartTime = weekendStartTime;
        this.weekendEndTime = weekendEndTime;
        this.addChargeUnit = addChargeUnit;
    }
}