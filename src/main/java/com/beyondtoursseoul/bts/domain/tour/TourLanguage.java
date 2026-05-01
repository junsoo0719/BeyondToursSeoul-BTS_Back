package com.beyondtoursseoul.bts.domain.tour;

import lombok.Getter;

@Getter
public enum TourLanguage {
    KOR("KorService2", "ko"),
    ENG("EngService2", "en"),
    JPN("JpnService2", "ja"),
    CHS("ChsService2", "zh-CN"), // 중국어 간체
    CHT("ChtService2", "zh-TW"); // 중국어 번체

    private final String serviceName;
    private final String googleLangCode;

    TourLanguage(String serviceName, String googleLangCode) {
        this.serviceName = serviceName;
        this.googleLangCode = googleLangCode;
    }
}
