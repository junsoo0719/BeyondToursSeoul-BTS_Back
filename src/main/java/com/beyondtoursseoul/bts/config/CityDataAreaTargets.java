package com.beyondtoursseoul.bts.config;

import java.util.List;

public final class CityDataAreaTargets {

    private CityDataAreaTargets() {
    }

    public static final List<CityDataAreaTarget> AREA_NAMES = List.of(
            new CityDataAreaTarget("광화문·덕수궁", "POI009", 37.5695270714, 126.9768018471),
            new CityDataAreaTarget("강남역", "POI014", 37.4983205000, 127.0284280403),
            new CityDataAreaTarget("명동 관광특구", "POI003", 37.5634110000, 126.9825820593)
    );
}
