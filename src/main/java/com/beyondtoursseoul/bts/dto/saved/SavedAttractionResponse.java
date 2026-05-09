package com.beyondtoursseoul.bts.dto.saved;

import com.beyondtoursseoul.bts.domain.Attraction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Getter
@Schema(description = "저장한 관광지")
public class SavedAttractionResponse {

    private final Long id;
    private final String name;
    private final String thumbnail;
    private final String address;
    private final Double lat;
    private final Double lng;
    private final OffsetDateTime savedAt;

    public SavedAttractionResponse(Attraction attraction, OffsetDateTime savedAt) {
        this.id = attraction.getId();
        this.name = attraction.getName();
        this.thumbnail = attraction.getThumbnail();
        this.address = attraction.getAddress();
        Point g = attraction.getGeom();
        if (g != null) {
            this.lng = g.getX();
            this.lat = g.getY();
        } else {
            this.lng = null;
            this.lat = null;
        }
        this.savedAt = savedAt;
    }
}
