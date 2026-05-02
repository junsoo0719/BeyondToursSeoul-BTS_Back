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

    /**
     * 물품보관함 테이블에서 사용하는 2글자 언어 코드를 반환합니다.
     */
    public String getLockerLangCode() {
        return switch (this) {
            case ENG -> "en";
            case JPN -> "ja";
            case CHS, CHT -> "zh";
            default -> "ko";
        };
    }

    /**
     * 문자열로부터 TourLanguage Enum을 찾아 반환합니다. (대소문자 무관)
     */
    public static TourLanguage fromCode(String code) {
        if (code == null) return KOR;
        for (TourLanguage lang : values()) {
            if (lang.name().equalsIgnoreCase(code) || lang.googleLangCode.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        // 2글자 코드로 들어올 경우에 대한 처리
        String lower = code.toLowerCase();
        if (lower.startsWith("en")) return ENG;
        if (lower.startsWith("ja")) return JPN;
        if (lower.startsWith("zh")) return CHS;
        return KOR;
    }
}
