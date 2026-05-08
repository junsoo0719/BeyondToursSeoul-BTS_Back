package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.dto.AiChatRequest;
import com.beyondtoursseoul.bts.dto.AiChatResponse;
import com.beyondtoursseoul.bts.service.rag.RagSearchService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GroqChatService {
    private static final Pattern PLACE_NAME_PATTERN = Pattern.compile("장소명\\s*:\\s*([^\\n\\r]+)");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("주소\\s*:\\s*([^\\n\\r]+)");
    private static final Pattern LOCAL_RATIO_PATTERN = Pattern.compile("로컬\\s*(\\d+)\\s*%");
    private static final List<String> SLOT_ORDER = List.of("아침", "오전 코스", "점심", "오후 코스", "저녁", "밤 코스");
    private static final int DEFAULT_LOCAL_RATIO = 50;

    private final RestClient restClient;
    private final RagSearchService ragSearchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.api.fallback-model:}")
    private String fallbackModel;

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

        List<RagSearchService.RagDocumentContext> ragDocs = searchRagDocuments(request);
        String ragContext = createRagContext(ragDocs);
        ArrayNode candidateArray = buildCandidateArray(ragDocs);

        String rawContent = requestCompletion(request, model, false, ragContext, candidateArray);
        log.info("[AI] model={}, rawResponse={}", model, truncateForLog(rawContent, 3000));
        AiChatResponse parsed = parseAiChatResponse(rawContent, model);
        parsed = ensureStructuredQuality(parsed, candidateArray);

        if (needsRetry(parsed)) {
            String retryModel = (fallbackModel != null && !fallbackModel.isBlank()) ? fallbackModel : model;
            String retryContent = requestCompletion(request, retryModel, true, ragContext, candidateArray);
            log.info("[AI] retry model={}, rawResponse={}", retryModel, truncateForLog(retryContent, 3000));
            AiChatResponse retried = parseAiChatResponse(retryContent, retryModel);
            retried = ensureStructuredQuality(retried, candidateArray);
            if (!needsRetry(retried)) {
                parsed = retried;
            }
        }

        log.info(
                "[AI] parsedResponse answerLength={}, hasStructured={}",
                parsed.getAnswer() == null ? 0 : parsed.getAnswer().length(),
                parsed.getStructured() != null
        );
        return parsed;
    }

    private List<RagSearchService.RagDocumentContext> searchRagDocuments(AiChatRequest request) {
        String searchQuery = createRagSearchQuery(request);
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "ko"
                : request.getLanguage();
        int localRatio = resolveLocalRatio(request);
        log.info("[AI] localRatio={}", localRatio);
        return ragSearchService.search(searchQuery, language, localRatio);
    }

    /**
     * localRatio 결정 순서:
     * 1. request.localRatio 필드 (프론트에서 명시 전달 시)
     * 2. 메시지 텍스트에서 "로컬 N%" 패턴 파싱
     * 3. 기본값 50
     */
    private int resolveLocalRatio(AiChatRequest request) {
        if (request.getLocalRatio() != null) {
            return Math.max(0, Math.min(100, request.getLocalRatio()));
        }
        if (request.getMessage() != null) {
            Matcher m = LOCAL_RATIO_PATTERN.matcher(request.getMessage());
            if (m.find()) {
                try {
                    return Math.max(0, Math.min(100, Integer.parseInt(m.group(1))));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return DEFAULT_LOCAL_RATIO;
    }

    private String requestCompletion(
            AiChatRequest request,
            String modelName,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray
    ) {
        GroqChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(createRequestBody(request, modelName, strictMode, ragContext, candidateArray))
                .retrieve()
                .body(GroqChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Groq 응답이 올바르지 않습니다.");
        }
        return response.getChoices().get(0).getMessage().getContent();
    }

    private Map<String, Object> createRequestBody(
            AiChatRequest request,
            String modelName,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray
    ) {
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "ko"
                : request.getLanguage();

        return Map.of(
                "model", modelName,
                "temperature", strictMode ? 0.0 : 0.2,
                "max_tokens", 1200,
                "response_format", Map.of("type", "json_object"),
                "messages", createMessages(request, language, strictMode, ragContext, candidateArray)
        );
    }

    private List<Map<String, String>> createMessages(
            AiChatRequest request,
            String language,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray
    ) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", createSystemPrompt(language)
        ));
        if (strictMode) {
            messages.add(Map.of(
                    "role", "system",
                    "content", createStrictRetryPrompt()
            ));
        }

        if (!ragContext.isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", ragContext
            ));
        }
        messages.add(Map.of(
                "role", "system",
                "content", createCandidatePrompt(candidateArray)
        ));

        if (request.getHistory() != null) {
            List<AiChatRequest.ChatHistoryMessage> validHistory = request.getHistory().stream()
                    .filter(this::isValidHistoryMessage)
                    .toList();
            // 최근 2턴(user+assistant 쌍 2개 = 최대 4메시지)만 유지
            int historyLimit = 4;
            int skip = Math.max(0, validHistory.size() - historyLimit);
            validHistory.stream()
                    .skip(skip)
                    .map(history -> Map.of(
                            "role", history.getRole().trim().toLowerCase(),
                            "content", truncate(history.getContent(), 300)
                    ))
                    .forEach(messages::add);
        }

        messages.add(Map.of(
                "role", "user",
                "content", request.getMessage()
        ));

        return messages;
    }

    // RAG context: 상위 8개, 제목+100자만 → ~400 토큰
    private String createRagContext(List<RagSearchService.RagDocumentContext> documents) {
        if (documents.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("PLACES:[");
        int limit = Math.min(documents.size(), 8);
        for (int i = 0; i < limit; i++) {
            RagSearchService.RagDocumentContext doc = documents.get(i);
            if (i > 0) context.append(",");
            context.append("{")
                    .append("\"cat\":\"").append(nullToEmpty(doc.category())).append("\"")
                    .append(",\"title\":\"").append(nullToEmpty(doc.title()).replace("\"", "")).append("\"")
                    .append(",\"info\":\"").append(truncate(nullToEmpty(doc.content()).replace("\"", "").replace("\n", " "), 100)).append("\"")
                    .append("}");
        }
        context.append("]");
        return context.toString();
    }

    // CANDIDATES: 상위 10개, compact JSON, n/a 키만 사용 → ~200 토큰
    private String createCandidatePrompt(ArrayNode candidates) {
        return "CANDIDATES:" + candidates.toString();
    }

    private ArrayNode buildCandidateArray(List<RagSearchService.RagDocumentContext> documents) {
        ArrayNode array = objectMapper.createArrayNode();
        Map<String, Boolean> dedupe = new LinkedHashMap<>();

        for (RagSearchService.RagDocumentContext doc : documents) {
            if (array.size() >= 10) break;

            String placeName = extractField(doc.content(), PLACE_NAME_PATTERN);
            if (placeName.isBlank()) {
                placeName = nullToEmpty(doc.title()).trim();
            }
            String address = extractField(doc.content(), ADDRESS_PATTERN);

            if (placeName.isBlank()) continue;
            String key = placeName.toLowerCase();
            if (dedupe.containsKey(key)) continue;
            dedupe.put(key, true);

            ObjectNode node = array.addObject();
            node.put("n", placeName);
            node.put("a", address);
            node.put("c", nullToEmpty(doc.category()));
        }
        return array;
    }

    private String extractField(String content, Pattern pattern) {
        if (content == null || content.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
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

    private AiChatResponse parseAiChatResponse(String rawContent, String modelName) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawContent));
            String answer = root.path("answer").asText("");
            JsonNode structuredNode;

            // 1순위: 루트에 "structured" 키가 있고, days 배열이 실제 존재하는 경우
            JsonNode embeddedStructured = root.path("structured");
            if (embeddedStructured.isObject() && embeddedStructured.path("days").isArray()) {
                structuredNode = embeddedStructured;
            // 2순위: 루트 자체가 days/budget 중 하나를 갖는 경우 (JSON-only 모드)
            } else if (root.isObject() && (root.has("days") || root.has("budget"))) {
                structuredNode = root;
            } else {
                structuredNode = createEmptyStructuredNode();
            }
            return new AiChatResponse(answer, toMap(structuredNode), modelName);
        } catch (Exception e) {
            log.warn("[AI] 응답 파싱 실패: {}", e.getMessage());
            return new AiChatResponse("", toMap(createEmptyStructuredNode()), modelName);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            log.warn("[AI] JsonNode → Map 변환 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private AiChatResponse ensureStructuredQuality(AiChatResponse response, ArrayNode candidates) {
        try {
            if (response == null || response.getStructured() == null) return response;
            if (!candidates.isArray() || candidates.isEmpty()) return response;

            Map<String, Object> structured = new LinkedHashMap<>(response.getStructured());
            Object daysObj = structured.get("days");
            if (!(daysObj instanceof List<?> dayList) || dayList.isEmpty()) return response;

            int candidateIndex = 0;
            for (Object dayRaw : dayList) {
                if (!(dayRaw instanceof Map<?, ?> dayMap)) continue;
                Map<String, Object> day = (Map<String, Object>) dayMap;
                Object slotsObj = day.get("slots");
                if (!(slotsObj instanceof List<?> slotList)) continue;

                for (int i = 0; i < slotList.size(); i++) {
                    Object slotRaw = slotList.get(i);
                    if (!(slotRaw instanceof Map<?, ?> slotMap)) continue;
                    Map<String, Object> slot = (Map<String, Object>) slotMap;

                    JsonNode candidate = candidates.get(candidateIndex % candidates.size());
                    candidateIndex++;

                    String type = str(slot.get("type"));
                    String label = str(slot.get("label"));
                    String placeName = str(slot.get("placeName"));
                    String address = str(slot.get("address"));
                    String reason = str(slot.get("reason"));

                    if (type.isBlank()) { type = slotTypeByIndex(i); slot.put("type", type); }
                    if (label.isBlank()) { slot.put("label", type); }
                    if (placeName.isBlank()) {
                        placeName = candidate.path("n").asText("").trim();
                        slot.put("placeName", placeName);
                    }
                    if (address.isBlank()) { slot.put("address", candidate.path("a").asText("")); }
                    if (reason.isBlank() && !placeName.isBlank()) {
                        slot.put("reason", placeName + " 중심으로 이동 동선을 고려한 추천 코스입니다.");
                    }
                }
            }
            return new AiChatResponse(response.getAnswer(), structured, response.getModel());
        } catch (Exception e) {
            log.warn("[AI] ensureStructuredQuality 실패, 원본 반환: {}", e.getMessage());
            return response;
        }
    }

    private String str(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private boolean needsRetry(AiChatResponse response) {
        if (response == null || response.getStructured() == null) return true;

        Object daysObj = response.getStructured().get("days");
        if (!(daysObj instanceof List<?> dayList) || dayList.isEmpty()) return true;

        int totalSlots = 0;
        int goodSlots = 0;
        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) continue;
            Object slotsObj = ((Map<String, Object>) dayMap).get("slots");
            if (!(slotsObj instanceof List<?> slotList)) continue;
            for (Object slotRaw : slotList) {
                if (!(slotRaw instanceof Map<?, ?> slotMap)) continue;
                Map<String, Object> slot = (Map<String, Object>) slotMap;
                totalSlots++;
                if (!str(slot.get("placeName")).isBlank()
                        && !str(slot.get("reason")).isBlank()
                        && !str(slot.get("type")).isBlank()) {
                    goodSlots++;
                }
            }
        }
        if (totalSlots == 0) return true;
        return (double) goodSlots / totalSlots < 0.6;
    }

    private String extractJsonObject(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int fenceEnd = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && fenceEnd > firstLineEnd) {
                trimmed = trimmed.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private JsonNode createEmptyStructuredNode() {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode summary = node.putObject("summary");
        summary.put("title", "");
        summary.putArray("route");
        node.putArray("days");
        ObjectNode budget = node.putObject("budget");
        budget.put("perPerson", "");
        budget.put("total", "");
        budget.put("note", "AI 응답을 구조화하지 못했습니다.");
        return node;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String truncateForLog(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
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
                Requested language: %s.

                You must return exactly one valid JSON object and nothing else.
                Do not wrap the JSON in markdown code fences.
                Return only this JSON shape:
                {
                  "days": [
                    {
                      "date": "YYYY-MM-DD",
                      "label": "1일차",
                      "slots": [
                        { "type": "...", "label": "...", "placeName": "...", "address": "...", "reason": "..." }
                      ]
                    }
                  ],
                  "budget": { "perPerson": "...", "total": "...", "note": "..." },
                  "summary": { "title": "...", "route": ["..."] }
                }

                Rules:
                - slot.type/label: one of 아침|오전 코스|점심|오후 코스|저녁|밤 코스
                - slot.label == slot.type
                - slot.placeName: use CANDIDATES[].n first, never empty
                - slot.address: use CANDIDATES[].a, empty if unknown
                - slot.reason: Korean sentence ≥10 chars
                - CANDIDATES key: n=name, a=address, c=category
                """.formatted(language);
    }

    private String createStrictRetryPrompt() {
        return """
                Retry mode: your previous JSON had too many empty fields.
                Fill every slot with non-empty values.
                Mandatory rules:
                - Each slot.type must be one of: 아침, 오전 코스, 점심, 오후 코스, 저녁, 밤 코스
                - Each slot.label must equal slot.type
                - slot.placeName must be a concrete place name in Seoul, never empty
                - slot.reason must be at least 10 Korean characters
                - slot.address should be filled when context contains it, otherwise use ""
                - Never output placeholder words or gibberish
                - Keep strict valid JSON only
                """;
    }

    private String slotTypeByIndex(int index) {
        if (index < 0 || index >= SLOT_ORDER.size()) {
            return "오후 코스";
        }
        return SLOT_ORDER.get(index);
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
