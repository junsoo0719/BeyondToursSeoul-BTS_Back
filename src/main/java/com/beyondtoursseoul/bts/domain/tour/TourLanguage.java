package com.beyondtoursseoul.bts.domain.tour;

import lombok.Getter;

@Getter
public enum TourLanguage {
    KOR("KorService2"),
    ENG("EngService2"),
    JPN("JpnService2"),
    CHS("ChsService2"), // 중국어 간체
    CHT("ChtService2"); // 중국어 번체
//    GER("GerService2"),
//    FRE("FreService2"),
//    SPN("SpnService2"),
//    RUS("RusService2");

    private final String serviceName;

    TourLanguage(String serviceName) {
        this.serviceName = serviceName;
    }
}
