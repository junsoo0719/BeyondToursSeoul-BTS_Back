package com.beyondtoursseoul.bts.dto.locker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 서울시 물품보관함 API 응답을 매핑하는 최상위 DTO
 */
@Getter
@NoArgsConstructor
@ToString
public class LockerResponseDto {

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
     */
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Item {
        
        // --- 공통 정보 (숫자/시간/기호) ---
        private String lckrId; // 보관함 고유 ID
        
        // 대문자인 필드는 자바 네이밍 컨벤션과 다르므로 @JsonProperty를 유지하는 것이 안전합니다.
        @JsonProperty("LAT")
        private Double lat; // 위도
        
        @JsonProperty("LOT")
        private Double lot; // 경도
        
        private Integer lckrCnt; // 총 개수
        private String wkdayOperBgngTm; // 평일 시작 시각
        private String wkdayOperEndTm; // 평일 종료 시각
        private String satOperBgngTm; // 주말 시작 시각
        private String satOperEndTm; // 주말 종료 시각
        private String addCrgUnitHr; // 추가 요금 단위 시간 (String으로 들어옴)
        
        // --- 다국어 번역 대상 정보 (텍스트) ---
        private String stnNm; // 역명
        private String lckrNm; // 보관함 이름
        private String lckrDtlLocNm; // 상세 위치
        private String utztnCrgExpln; // 기본 요금 설명
        private String addCrgExpln; // 추가 요금 설명
        private String kpngLmtLckrExpln; // 제한 물품 설명
        
        // 크기 정보
        private String lckrWdthLenExpln; // 가로 길이
        private String lckrDpthExpln; // 깊이
        private String lckrHgtExpln; // 높이
    }
}
