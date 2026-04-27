package com.beyondtoursseoul.bts.service.translation;

import java.util.List;

/**
 * 번역 Service 클래스 추상화
 * */
public interface TranslationService {
    String translate(String text, String sourceLang, String targetLang);
    List<String> translateBatch(List<String> texts, String sourceLang, String targetLang);
}
