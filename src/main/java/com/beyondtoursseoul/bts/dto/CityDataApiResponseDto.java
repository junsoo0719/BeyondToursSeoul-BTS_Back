package com.beyondtoursseoul.bts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CityDataApiResponseDto {

    @JsonProperty("list_total_count")
    private int listTotalCount;

    @JsonProperty("CITYDATA")
    private CityData cityData;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CityData {

        @JsonProperty("AREA_NM")
        private String areaName;

        @JsonProperty("AREA_CD")
        private String areaCode;

        @JsonProperty("LIVE_PPLTN_STTS")
        private List<LivePopulationStatus> livePopulationStatuses;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LivePopulationStatus {

        @JsonProperty("AREA_NM")
        private String areaName;

        @JsonProperty("AREA_CD")
        private String areaCode;

        @JsonProperty("AREA_CONGEST_LVL")
        private String congestionLevel;

        @JsonProperty("AREA_CONGEST_MSG")
        private String congestionMessage;

        @JsonProperty("AREA_PPLTN_MIN")
        private String areaPpltnMin;

        @JsonProperty("AREA_PPLTN_MAX")
        private String areaPpltnMax;

        @JsonProperty("PPLTN_TIME")
        private String populationTime;

        @JsonProperty("FCST_YN")
        private String forecastYn;

        @JsonProperty("FCST_PPLTN")
        private List<ForecastPopulation> forecastPopulations;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastPopulation {

        @JsonProperty("FCST_TIME")
        private String forecastTime;

        @JsonProperty("FCST_CONGEST_LVL")
        private String forecastCongestionLevel;

        @JsonProperty("FCST_PPLTN_MIN")
        private String forecastPpltnMin;

        @JsonProperty("FCST_PPLTN_MAX")
        private String forecastPpltnMax;
    }
}
