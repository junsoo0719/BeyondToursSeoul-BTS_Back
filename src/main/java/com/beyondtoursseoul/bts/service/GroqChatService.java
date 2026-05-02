package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.dto.AiChatRequest;
import com.beyondtoursseoul.bts.dto.AiChatResponse;
import com.beyondtoursseoul.bts.service.rag.RagSearchService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GroqChatService {

    private final RestClient restClient;
    private final RagSearchService ragSearchService;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    public GroqChatService(
            RagSearchService ragSearchService,
            @Value("${groq.api.base-url:https://api.groq.com/openai/v1}") String baseUrl
    ) {
        this.ragSearchService = ragSearchService;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY가 설정되어 있지 않습니다.");
        }

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalStateException("message는 필수입니다.");
        }

        GroqChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(createRequestBody(request))
                .retrieve()
                .body(GroqChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Groq 응답이 올바르지 않습니다.");
        }

        return new AiChatResponse(response.getChoices().get(0).getMessage().getContent(), model);
    }

    private Map<String, Object> createRequestBody(AiChatRequest request) {
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "ko"
                : request.getLanguage();

        return Map.of(
                "model", model,
                "temperature", 0.7,
                "max_tokens", 800,
                "messages", createMessages(request, language)
        );
    }

    private List<Map<String, String>> createMessages(AiChatRequest request, String language) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", createSystemPrompt(language)
        ));

        String ragContext = createRagContext(request, language);
        if (!ragContext.isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", ragContext
            ));
        }

        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(this::isValidHistoryMessage)
                    .map(history -> Map.of(
                            "role", history.getRole().trim().toLowerCase(),
                            "content", history.getContent()
                    ))
                    .forEach(messages::add);
        }

        messages.add(Map.of(
                "role", "user",
                "content", request.getMessage()
        ));

        return messages;
    }

    private String createRagContext(AiChatRequest request, String language) {
        String searchQuery = createRagSearchQuery(request);
        List<RagSearchService.RagDocumentContext> documents = ragSearchService.search(searchQuery, language);
        if (documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("""
                Use the following database search results as trusted reference context.
                Prefer this context over general model knowledge when recommending Seoul places, events, lockers, or itineraries.
                Do not invent details that are not present in either the user request or this context.

                [RAG_CONTEXT]
                """);

        for (int i = 0; i < documents.size(); i++) {
            RagSearchService.RagDocumentContext document = documents.get(i);
            context.append("\n")
                    .append(i + 1)
                    .append(". category: ").append(nullToEmpty(document.category()))
                    .append("\n")
                    .append("type: ").append(nullToEmpty(document.sourceType()))
                    .append("\n")
                    .append("title: ").append(nullToEmpty(document.title()))
                    .append("\n")
                    .append("dongCode: ").append(nullToEmpty(document.dongCode()))
                    .append("\n")
                    .append("score: ").append(document.matchScore())
                    .append("\n")
                    .append("content: ").append(truncate(nullToEmpty(document.content()), 450))
                    .append("\n");
        }

        return context.toString();
    }

    private String createRagSearchQuery(AiChatRequest request) {
        List<String> parts = new ArrayList<>();

        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(this::isValidHistoryMessage)
                    .skip(Math.max(0, request.getHistory().stream()
                            .filter(this::isValidHistoryMessage)
                            .count() - 4))
                    .map(AiChatRequest.ChatHistoryMessage::getContent)
                    .map(content -> truncate(content, 300))
                    .forEach(parts::add);
        }

        parts.add(request.getMessage());
        return String.join("\n", parts);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isValidHistoryMessage(AiChatRequest.ChatHistoryMessage message) {
        if (message == null || message.getRole() == null || message.getContent() == null
                || message.getContent().isBlank()) {
            return false;
        }

        String role = message.getRole().trim().toLowerCase();
        return "user".equals(role) || "assistant".equals(role);
    }

    private String createSystemPrompt(String language) {
        return """
                You are Beyond Tours Seoul's AI travel assistant.
                Help foreign visitors explore Seoul with practical, friendly, and accurate recommendations.
                Answer in the requested language: %s.
                If you are not sure about real-time availability, tell the user to check official sources.

                When the user asks for a travel itinerary, follow these planning rules:
                - Always start with "전체 요약 코스" before the day-by-day plan.
                - The summary must show the route at a glance, such as "1일차: 홍대/망원 -> 한강 야경, 2일차: 종로/성수 -> 루프탑".
                - Then write each day using Korean slot labels only.
                - For a full day, use this exact order: 아침 -> 오전 코스 -> 점심 -> 오후 코스 -> 저녁 -> 밤 코스.
                - "아침", "점심", and "저녁" are restaurant/meal slots.
                - "오전 코스", "오후 코스", and "밤 코스" are activity/place slots. Do not fill these slots with restaurants unless the user explicitly asks for a food-only itinerary.
                - On the first day, remove every slot before the user's arrival time. If arrival is 14:00, start from "오후 코스" or later.
                - On the last day, remove every slot that is unrealistic before the departure time. If departure is 10:00, skip sightseeing and add only a short departure note.
                - Do not force all slots when the day is shortened by arrival or departure time.
                - Do not repeat the same place in multiple slots unless the user explicitly asks to revisit it.
                - Use at most three restaurants per full day: 아침, 점심, 저녁.
                - Use roughly three activity/place slots per full day: 오전 코스, 오후 코스, 밤 코스.
                - Prefer a balanced mix of restaurants, attractions, cultural events, shopping/K-pop spots, and night-view/bar options when the user asks for multiple themes.
                - Keep the route realistic. Group places by nearby districts when possible.
                - For each slot, include place name, district/address if available, and a short reason.
                - If the RAG context is heavily biased toward one category, still build a balanced itinerary and mention when there is not enough supporting data for a requested theme.
                - Do not write budget estimates inside each day or each slot.
                - After all day-by-day plans are finished, add "준비물" once.
                - After "준비물", add "예상 예산" once as the final section of the entire answer.
                - "예상 예산" must appear only once at the very end of the response.
                - In the final "예상 예산" section, estimate per-person and total-party ranges when the number of travelers is known.
                - Base the final budget on meals, admission/activity fees, local transportation/parking, cafe/bar spending, and miscellaneous costs.
                - Do not include flights or accommodation unless the user provided enough information.
                - If exact prices are not available in the context, provide a reasonable range and clearly say it is an estimate.
                """.formatted(language);
    }

    @Getter
    @NoArgsConstructor
    private static class GroqChatResponse {
        private List<Choice> choices;
    }

    @Getter
    @NoArgsConstructor
    private static class Choice {
        private Message message;
    }

    @Getter
    @NoArgsConstructor
    private static class Message {
        private String role;

        @JsonProperty("content")
        private String content;
    }
}
