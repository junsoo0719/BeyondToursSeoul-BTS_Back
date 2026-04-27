package com.beyondtoursseoul.bts.service.translation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GoogleTranslationServiceTest {

    @Autowired
    private TranslationService translationService;

    @Test
    void 구글_번역_테스트() {
        String sourceText = "안녕하세요";
        String result = translationService.translate(sourceText, "ko", "en");

        System.out.println("결과: " + result);
        assertThat(result).containsIgnoringCase("Hello");
    }

}