package com.beyondtoursseoul.bts.dto.attraction;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

@Getter
@Schema(description = "관광지 목록 항목")
public class AttractionSummaryResponse {

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

    @Schema(description = "해당 날짜·시간대의 찐로컬 지수 (0~1)")
    private final BigDecimal score;

    public AttractionSummaryResponse(Attraction attraction, AttractionLocalScore score,
                                     String cat1Name, String cat2Name, String cat3Name,
                                     AttractionTranslation translation) {
        Point geom = attraction.getGeom();
        if (geom == null) {
            throw new IllegalStateException("관광지 좌표 데이터가 없습니다: id=" + attraction.getId());
        }
        this.id = attraction.getId();
        this.name = translation != null && translation.getName() != null
                ? translation.getName() : attraction.getName();
        this.thumbnail = attraction.getThumbnail();
        this.cat1Name = cat1Name;
        this.cat2Name = cat2Name;
        this.cat3Name = cat3Name;
        this.address = translation != null && translation.getAddress() != null
                ? translation.getAddress() : attraction.getAddress();
        this.lng = geom.getX();
        this.lat = geom.getY();
        this.score = score != null ? score.getScore() : null;
    }
}
