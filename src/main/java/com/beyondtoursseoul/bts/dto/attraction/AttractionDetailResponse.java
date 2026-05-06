package com.beyondtoursseoul.bts.dto.attraction;

import com.beyondtoursseoul.bts.domain.Attraction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Schema(description = "관광지 상세 정보")
public class AttractionDetailResponse {

    @Schema(description = "관광지 ID")
    private final Long id;

    @Schema(description = "관광지명")
    private final String name;

    @Schema(description = "대표 썸네일 이미지 URL")
    private final String thumbnail;

    @Schema(description = "대분류명 (예: 문화관광)")
    private final String cat1Name;

    @Schema(description = "중분류명 (예: 전시시설)")
    private final String cat2Name;

    @Schema(description = "소분류명 (예: 기념관)")
    private final String cat3Name;

    @Schema(description = "주소")
    private final String address;

    @Schema(description = "경도")
    private final double lng;

    @Schema(description = "위도")
    private final double lat;

    @Schema(description = "전화번호")
    private final String tel;

    @Schema(description = "관광지 소개글")
    private final String overview;

    @Schema(description = "운영시간")
    private final String operatingHours;

    @Schema(description = "찐로컬 지수 데이터 존재 여부. false면 scores는 빈 Map")
    private final boolean scoresAvailable;

    @Schema(description = "시간대별 찐로컬 지수. key: morning/lunch/afternoon/evening/night, value: 0~1")
    private final Map<String, BigDecimal> scores;

    public AttractionDetailResponse(Attraction attraction,
                                    String cat1Name, String cat2Name, String cat3Name,
                                    Map<String, BigDecimal> scores) {
        Point geom = attraction.getGeom();
        if (geom == null) {
            throw new IllegalStateException("관광지 좌표 데이터가 없습니다: id=" + attraction.getId());
        }
        this.id = attraction.getId();
        this.name = attraction.getName();
        this.thumbnail = attraction.getThumbnail();
        this.cat1Name = cat1Name;
        this.cat2Name = cat2Name;
        this.cat3Name = cat3Name;
        this.address = attraction.getAddress();
        this.lng = geom.getX();
        this.lat = geom.getY();
        this.tel = attraction.getTel();
        this.overview = attraction.getOverview();
        this.operatingHours = attraction.getOperatingHours();
        this.scoresAvailable = scores != null && !scores.isEmpty();
        this.scores = scores;
    }
}
