package com.beyondtoursseoul.bts.dto.locker;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 서울시 물품보관함 API 응답을 매핑하는 외부 API 전용 DTO
 */
@Getter
@NoArgsConstructor
@ToString
public class LockerApiResponseDto {

    private Response response;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Body {
        private Items items;
        private Integer pageNo;
        private Integer numOfRows;
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Items {
        private List<Item> item;
    }

    /**
     * 실제 보관함 1개에 대한 상세 데이터
     * @JsonAlias를 사용하여 외부 API의 알아보기 쉬운 필드명으로 수정
     * @JsonProperty는 스웨거에 적용해주지 않음 alias는 읽을 때만, property는 읽고 쓰기
     */
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Item {
        
        @JsonAlias("lckrId")
        private String lockerId; // 보관함 고유 ID
        
        @JsonAlias("LAT")
        private Double latitude; // 위도 (Latitude)
        
        @JsonAlias("LOT")
        private Double longitude; // 경도 (Longitude)
        
        @JsonAlias("lckrCnt")
        private Integer totalCount; // 전체 보관함 개수
        
        @JsonAlias("wkdayOperBgngTm")
        private String weekdayStartTime; // 평일 운영 시작 시각
        
        @JsonAlias("wkdayOperEndTm")
        private String weekdayEndTime; // 평일 운영 종료 시각
        
        @JsonAlias("satOperBgngTm")
        private String weekendStartTime; // 주말/공휴일 운영 시작 시각
        
        @JsonAlias("satOperEndTm")
        private String weekendEndTime; // 주말/공휴일 운영 종료 시각
        
        @JsonAlias("addCrgUnitHr")
        private String addChargeUnit; // 추가 요금 단위 시간
        
        @JsonAlias("stnNm")
        private String stationName; // 지하철역 이름
        
        @JsonAlias("lckrNm")
        private String lockerName; // 보관함 이름
        
        @JsonAlias("lckrDtlLocNm")
        private String lockerDetailLocation; // 보관함 상세 설치 위치
        
        @JsonAlias("utztnCrgExpln")
        private String basicChargeInfo; // 기본 이용 요금 설명
        
        @JsonAlias("addCrgExpln")
        private String addChargeInfo; // 추가 이용 요금 설명
        
        @JsonAlias("kpngLmtLckrExpln")
        private String keepLimitInfo; // 보관 제한 물품 안내
        
        @JsonAlias("lckrWdthLenExpln")
        private String lockerWidth; // 보관함 가로 길이 정보
        
        @JsonAlias("lckrDpthExpln")
        private String lockerDepth; // 보관함 깊이 정보
        
        @JsonAlias("lckrHgtExpln")
        private String lockerHeight; // 보관함 높이 정보
    }
}
