package com.beyondtoursseoul.bts.service.translation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class GoogleTranslationService implements TranslationService {

    private static final String GOOGLE_TRANSLATE_URL =
            "https://translation.googleapis.com/language/translate/v2";

    private final RestClient restClient;

    @Value("${google.translation.api.key}")
    private String apiKey;

    public GoogleTranslationService() {
        this.restClient = RestClient.builder().baseUrl(GOOGLE_TRANSLATE_URL).build();
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) return "";

        try {
            GoogleTranslationResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("q", text)
                            .queryParam("source", sourceLang)
                            .queryParam("target", targetLang)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve().body(GoogleTranslationResponse.class);

            if (response != null && response.getData() != null) {
                return response.getData().getTranslations().get(0).getTranslatedText();
            }
        } catch (Exception e) {
            log.info("번역 실패: {}", e.getMessage());
        }

        return text;
    }

    @Override
    public List<String> translateBatch(List<String> texts, String sourceLang, String targetLang) {
        return List.of();
    }


    /// 내부에 dto 선언 - 여기서만 사용함
    /// json 형식 따라 설정
    @Getter // response에서 data필드 가져올 때 사용
    @NoArgsConstructor  // jackson에서 텅빈 박스를 먼저 가져오고 거기에 채워넣는 방식을 사용, 그래 없으면 에러
    private static class GoogleTranslationResponse {
        private Data data;

        @Getter
        @NoArgsConstructor
        private static class Data {
            private List<Translation> translations;
        }

        @Getter
        @NoArgsConstructor
        private static class Translation {
            private String translatedText;
        }
    }
}
