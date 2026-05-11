package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.AiChatRequest;
import com.beyondtoursseoul.bts.dto.AiChatResponse;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.AttractionTranslationRepository;
import com.beyondtoursseoul.bts.repository.locker.LockerRepository;
import com.beyondtoursseoul.bts.repository.locker.LockerTranslationRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GroqChatService {
    private static final int RAG_CONTEXT_LIMIT = 12;
    private static final int CANDIDATE_LIMIT_MIN = 20;
    private static final int CANDIDATE_LIMIT_MAX = 200;
    private static final int MAX_TRAVEL_MINUTES = 45;
    private static final double CITY_SPEED_KMH = 18.0;
    private static final Pattern PLACE_NAME_PATTERN = Pattern.compile("장소명\\s*:\\s*([^\\n\\r]+)");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("주소\\s*:\\s*([^\\n\\r]+)");
    private static final Pattern OPERATING_HOURS_PATTERN = Pattern.compile("\\[운영시간\\]\\s*([^\\n\\r]+)");
    private static final Pattern CONTENT_IMAGE_PATTERN = Pattern.compile("(?i)(?:썸네일|thumbnail|대표\\s*이미지|image)\\s*:?\\s*(https?://\\S+)");
    private static final Pattern METADATA_IMAGE_PATTERN = Pattern.compile(
            "(?i)\"(?:thumbnail|imageurl|image_url|firstimage|first_image|photo|photourl|first_image2)\"\\s*:\\s*\"(https?://[^\"]+)\""
    );
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(?:~|-|–|—)\\s*(\\d{1,2})(?::(\\d{2}))?");
    private static final Pattern LOCAL_RATIO_PATTERN = Pattern.compile("로컬\\s*(\\d+)\\s*%");
    /** 사용자 요약(히스토리)·메시지에서 도착/출발 시각 파싱 (다국어 UI 문자열 포함) */
    private static final Pattern ARRIVAL_TIME_PATTERN = Pattern.compile(
            "(?:도착|arrival|arrive)\\s*[:：]?\\s*(\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DEPARTURE_TIME_PATTERN = Pattern.compile(
            "(?:출발|departure|depart)\\s*[:：]?\\s*(\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*(?:~|〜|–|—|-)\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final List<String> SLOT_ORDER = List.of("아침", "오전 코스", "점심", "오후 코스", "저녁", "밤 코스");
    private static final int DEFAULT_LOCAL_RATIO = 50;

    private final RestClient restClient;
    private final RagSearchService ragSearchService;
    private final AttractionRepository attractionRepository;
    private final AttractionTranslationRepository attractionTranslationRepository;
    private final TourApiEventRepository tourApiEventRepository;
    private final TourApiEventTranslationRepository tourApiEventTranslationRepository;
    private final LockerRepository lockerRepository;
    private final LockerTranslationRepository lockerTranslationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.api.fallback-model:}")
    private String fallbackModel;

    public GroqChatService(
            RagSearchService ragSearchService,
            AttractionRepository attractionRepository,
            AttractionTranslationRepository attractionTranslationRepository,
            TourApiEventRepository tourApiEventRepository,
            TourApiEventTranslationRepository tourApiEventTranslationRepository,
            LockerRepository lockerRepository,
            LockerTranslationRepository lockerTranslationRepository,
            @Value("${groq.api.base-url:https://api.groq.com/openai/v1}") String baseUrl
    ) {
        this.ragSearchService = ragSearchService;
        this.attractionRepository = attractionRepository;
        this.attractionTranslationRepository = attractionTranslationRepository;
        this.tourApiEventRepository = tourApiEventRepository;
        this.tourApiEventTranslationRepository = tourApiEventTranslationRepository;
        this.lockerRepository = lockerRepository;
        this.lockerTranslationRepository = lockerTranslationRepository;
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
        ArrayNode candidateArray = buildCandidateArray(ragDocs, request);

        String rawContent = requestCompletion(request, model, false, ragContext, candidateArray);
        log.info("[AI] model={}, rawResponse={}", model, truncateForLog(rawContent, 3000));
        AiChatResponse parsed = parseAiChatResponse(rawContent, model);
        parsed = ensureStructuredQuality(parsed, candidateArray, request);

        if (needsRetry(parsed)) {
            String retryModel = (fallbackModel != null && !fallbackModel.isBlank()) ? fallbackModel : model;
            String retryContent = requestCompletion(request, retryModel, true, ragContext, candidateArray);
            log.info("[AI] retry model={}, rawResponse={}", retryModel, truncateForLog(retryContent, 3000));
            AiChatResponse retried = parseAiChatResponse(retryContent, retryModel);
            retried = ensureStructuredQuality(retried, candidateArray, request);
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
            // 수정 플로우: 최근 대화를 조금 더 유지 (최대 8메시지 ≈ 4턴)
            int historyLimit = 8;
            int skip = Math.max(0, validHistory.size() - historyLimit);
            validHistory.stream()
                    .skip(skip)
                    .map(history -> Map.of(
                            "role", history.getRole().trim().toLowerCase(),
                            "content", truncate(history.getContent(), 520)
                    ))
                    .forEach(messages::add);
        }

        messages.add(Map.of(
                "role", "user",
                "content", request.getMessage()
        ));

        return messages;
    }

    // RAG context: 상위 12개, 제목+100자만
    private String createRagContext(List<RagSearchService.RagDocumentContext> documents) {
        if (documents.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("PLACES:[");
        int limit = Math.min(documents.size(), RAG_CONTEXT_LIMIT);
        for (int i = 0; i < limit; i++) {
            RagSearchService.RagDocumentContext doc = documents.get(i);
            if (i > 0) context.append(",");
            context.append("{")
                    .append("\"cat\":\"").append(nullToEmpty(doc.category())).append("\"")
                    .append(",\"title\":\"").append(nullToEmpty(doc.title()).replace("\"", "")).append("\"")
                    .append(",\"info\":\"")
                    .append(truncate(
                            slimRagContentForPrompt(nullToEmpty(doc.content()))
                                    .replace("\"", "")
                                    .replace("\n", " "),
                            100))
                    .append("\"")
                    .append("}");
        }
        context.append("]");
        return context.toString();
    }

    // CANDIDATES: RAG에서 넘어온 문서 수에 맞춰 compact JSON (n/a 키), 상한은 토큰 고려
    private String createCandidatePrompt(ArrayNode candidates) {
        ArrayNode promptCandidates = objectMapper.createArrayNode();
        for (JsonNode candidate : candidates) {
            ObjectNode node = promptCandidates.addObject();
            node.put("n", candidate.path("n").asText(""));
            node.put("a", candidate.path("a").asText(""));
            node.put("c", candidate.path("c").asText(""));
            node.put("h", candidate.path("h").asText(""));
            node.put("st", candidate.path("st").asText(""));
            node.put("sid", candidate.path("sid").asText(""));
            if (!candidate.path("lat").isMissingNode() && !candidate.path("lat").isNull()) {
                node.put("lat", candidate.path("lat").asDouble());
            }
            if (!candidate.path("lng").isMissingNode() && !candidate.path("lng").isNull()) {
                node.put("lng", candidate.path("lng").asDouble());
            }
            if (!candidate.path("ls").isMissingNode() && !candidate.path("ls").isNull()) {
                node.put("ls", candidate.path("ls").asDouble());
            }
        }
        return "CANDIDATES:" + promptCandidates;
    }

    private int candidateArrayLimit(List<RagSearchService.RagDocumentContext> documents) {
        if (documents == null || documents.isEmpty()) {
            return CANDIDATE_LIMIT_MIN;
        }
        int n = documents.size();
        return Math.min(CANDIDATE_LIMIT_MAX, Math.max(CANDIDATE_LIMIT_MIN, n));
    }

    private ArrayNode buildCandidateArray(List<RagSearchService.RagDocumentContext> documents, AiChatRequest request) {
        ArrayNode array = objectMapper.createArrayNode();
        Map<String, Boolean> dedupe = new LinkedHashMap<>();
        TripDateRange tripDateRange = resolveTripDateRange(request);
        int candidateLimit = candidateArrayLimit(documents);

        for (RagSearchService.RagDocumentContext doc : documents) {
            if (array.size() >= candidateLimit) break;
            if (tripDateRange != null && isEventDoc(doc) && !isEventInTripWindow(doc, tripDateRange)) {
                continue;
            }

            String body = slimRagContentForPrompt(nullToEmpty(doc.content()));
            String thumbnail = extractThumbnail(body, doc.metadata());

            String placeName = extractField(body, PLACE_NAME_PATTERN);
            if (placeName.isBlank()) {
                placeName = nullToEmpty(doc.title()).trim();
            }
            String address = extractField(body, ADDRESS_PATTERN);

            if (placeName.isBlank()) continue;
            String key = placeName.toLowerCase();
            if (dedupe.containsKey(key)) continue;
            dedupe.put(key, true);

            ObjectNode node = array.addObject();
            node.put("n", placeName);
            node.put("a", address);
            node.put("c", nullToEmpty(doc.category()));
            node.put("h", extractField(body, OPERATING_HOURS_PATTERN));
            node.put("st", nullToEmpty(doc.sourceType()));
            node.put("sid", nullToEmpty(doc.sourceId()));
            if (!thumbnail.isBlank()) {
                node.put("img", thumbnail);
            }
            if (doc.latitude() != null) node.put("lat", doc.latitude());
            if (doc.longitude() != null) node.put("lng", doc.longitude());
            if (doc.localScore() != null) {
                node.put("ls", doc.localScore());
            }
        }
        return array;
    }

    private TripDateRange resolveTripDateRange(AiChatRequest request) {
        if (request == null) return null;
        TripDateRange fromMessage = extractDateRange(request.getMessage());
        if (fromMessage != null) return fromMessage;
        if (request.getHistory() == null) return null;
        for (AiChatRequest.ChatHistoryMessage historyMessage : request.getHistory()) {
            if (!isValidHistoryMessage(historyMessage)) continue;
            TripDateRange fromHistory = extractDateRange(historyMessage.getContent());
            if (fromHistory != null) return fromHistory;
        }
        return null;
    }

    private TripDateRange extractDateRange(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = DATE_RANGE_PATTERN.matcher(value);
        if (!matcher.find()) return null;
        try {
            LocalDate start = LocalDate.parse(matcher.group(1));
            LocalDate end = LocalDate.parse(matcher.group(2));
            if (end.isBefore(start)) return null;
            return new TripDateRange(start, end);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isEventDoc(RagSearchService.RagDocumentContext doc) {
        String sourceType = nullToEmpty(doc.sourceType()).toLowerCase();
        String category = nullToEmpty(doc.category()).toLowerCase();
        return sourceType.contains("event") || category.contains("event");
    }

    private boolean isEventInTripWindow(RagSearchService.RagDocumentContext doc, TripDateRange tripDateRange) {
        String eventText = String.join(" ",
                nullToEmpty(doc.title()),
                slimRagContentForPrompt(nullToEmpty(doc.content())),
                nullToEmpty(doc.metadata())
        );
        TripDateRange eventRange = extractDateRange(eventText);
        if (eventRange == null) return true;
        return !eventRange.end().isBefore(tripDateRange.start()) && !eventRange.start().isAfter(tripDateRange.end());
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

    private String extractThumbnail(String content, String metadata) {
        String fromMetadata = extractByPattern(nullToEmpty(metadata), METADATA_IMAGE_PATTERN);
        if (!fromMetadata.isBlank()) {
            return fromMetadata;
        }
        return extractByPattern(nullToEmpty(content), CONTENT_IMAGE_PATTERN);
    }

    private String extractByPattern(String source, Pattern pattern) {
        if (source == null || source.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
    }

    private String createRagSearchQuery(AiChatRequest request) {
        List<String> parts = new ArrayList<>();

        if (request.getHistory() != null) {
            List<AiChatRequest.ChatHistoryMessage> valid = request.getHistory().stream()
                    .filter(this::isValidHistoryMessage)
                    .toList();
            int ragHistoryLimit = 8;
            int skip = Math.max(0, valid.size() - ragHistoryLimit);
            valid.stream()
                    .skip(skip)
                    .map(AiChatRequest.ChatHistoryMessage::getContent)
                    .map(content -> truncate(content, 520))
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
    private AiChatResponse ensureStructuredQuality(AiChatResponse response, ArrayNode candidates, AiChatRequest request) {
        try {
            if (response == null || response.getStructured() == null) return response;
            if (!candidates.isArray() || candidates.isEmpty()) return response;

            Map<String, Object> structured = new LinkedHashMap<>(response.getStructured());
            Object daysObj = structured.get("days");
            if (!(daysObj instanceof List<?> dayList) || dayList.isEmpty()) return response;

            int candidateIndex = 0;
            Set<String> usedPlaceNames = new HashSet<>();
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

                    if (type.isBlank()) { type = slotTypeByIndex(i); slot.put("type", type); }
                    if (label.isBlank()) { slot.put("label", type); }
                    boolean mealSlot = isMealSlot(type, label);
                    JsonNode matchedCandidate = findCandidateByPlaceName(candidates, placeName);
                    boolean duplicateName = !placeName.isBlank() && usedPlaceNames.contains(placeName.toLowerCase());
                    boolean closedForSlot = matchedCandidate != null && !isLikelyOpenForSlot(matchedCandidate, type, label);
                    boolean weakMealMatch = mealSlot
                            && matchedCandidate != null
                            && !isRestaurantCategory(matchedCandidate.path("c").asText(""));

                    if (placeName.isBlank() || duplicateName || matchedCandidate == null || closedForSlot || weakMealMatch) {
                        JsonNode uniqueCandidate = findPreferredCandidate(
                                candidates,
                                usedPlaceNames,
                                candidateIndex,
                                mealSlot,
                                !mealSlot,
                                type,
                                label
                        );
                        if (uniqueCandidate != null) {
                            candidate = uniqueCandidate;
                            placeName = candidate.path("n").asText("").trim();
                            address = candidate.path("a").asText("");
                            matchedCandidate = candidate;
                        } else if (placeName.isBlank()) {
                            placeName = candidate.path("n").asText("").trim();
                        }
                        slot.put("placeName", placeName);
                    }
                    if (address.isBlank()) { slot.put("address", candidate.path("a").asText("")); }
                    if (matchedCandidate != null) {
                        if (str(slot.get("address")).isBlank()) {
                            slot.put("address", matchedCandidate.path("a").asText(""));
                        }
                        putSlotSourceInfo(slot, matchedCandidate);
                        putSlotCoordinates(slot, matchedCandidate);
                    } else {
                        putSlotSourceInfo(slot, candidate);
                        putSlotCoordinates(slot, candidate);
                    }
                    slot.put("reason", "");
                    if (!placeName.isBlank()) {
                        usedPlaceNames.add(placeName.toLowerCase());
                    }
                }
            }
            applyFlightTimeWindow(dayList, request);
            optimizeDailyTravel(dayList, candidates);
            optimizeCrossDayTravel(dayList, candidates);
            optimizeDailyTravel(dayList, candidates);
            localizeSlotsBySource(dayList, request.getLanguage());
            clearAllSlotReasons(dayList);
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
    private void localizeSlotsBySource(List<?> dayList, String requestLanguage) {
        if (dayList == null || dayList.isEmpty()) return;

        TourLanguage tourLanguage = resolveTourLanguage(requestLanguage);
        String attractionLang = resolveAttractionLang(requestLanguage);
        String lockerLangCode = tourLanguage.getLockerLangCode();

        Set<Long> attractionIds = new HashSet<>();
        Set<Long> eventIds = new HashSet<>();
        Set<Long> lockerIds = new HashSet<>();

        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) continue;
            for (Map<String, Object> slot : mutableSlots((Map<String, Object>) dayMap)) {
                String sourceType = str(slot.get("sourceType")).toLowerCase();
                Long sourceId = parseLongOrNull(str(slot.get("sourceId")));
                if (sourceId == null) continue;
                if (sourceType.contains("attraction")) attractionIds.add(sourceId);
                else if (sourceType.contains("event")) eventIds.add(sourceId);
                else if (sourceType.contains("locker")) lockerIds.add(sourceId);
            }
        }

        Map<Long, Attraction> attractionMap = attractionIds.isEmpty()
                ? Map.of()
                : attractionRepository.findAllById(attractionIds).stream()
                .collect(LinkedHashMap::new, (m, a) -> m.put(a.getId(), a), LinkedHashMap::putAll);
        Map<Long, AttractionTranslation> attractionTranslationMap = (!attractionIds.isEmpty() && !"ko".equals(attractionLang))
                ? attractionTranslationRepository.findByIdAttractionIdInAndIdLang(new ArrayList<>(attractionIds), attractionLang).stream()
                .collect(LinkedHashMap::new, (m, t) -> m.put(t.getId().getAttractionId(), t), LinkedHashMap::putAll)
                : Map.of();

        Map<Long, TourApiEvent> eventMap = eventIds.isEmpty()
                ? Map.of()
                : tourApiEventRepository.findAllById(eventIds).stream()
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getContentId(), e), LinkedHashMap::putAll);
        Map<Long, TourApiEventTranslation> eventTranslationMap = new LinkedHashMap<>();
        if (!eventMap.isEmpty()) {
            List<TourApiEvent> events = new ArrayList<>(eventMap.values());
            List<TourLanguage> languages = tourLanguage == TourLanguage.KOR
                    ? List.of(TourLanguage.KOR)
                    : List.of(tourLanguage, TourLanguage.KOR);
            for (TourApiEventTranslation tr : tourApiEventTranslationRepository.findByEventInAndLanguageIn(events, languages)) {
                Long id = tr.getEvent().getContentId();
                TourApiEventTranslation existing = eventTranslationMap.get(id);
                if (existing == null || (existing.getLanguage() != tourLanguage && tr.getLanguage() == tourLanguage)) {
                    eventTranslationMap.put(id, tr);
                }
            }
        }

        Map<Long, Locker> lockerMap = lockerIds.isEmpty()
                ? Map.of()
                : lockerRepository.findAllById(lockerIds).stream()
                .collect(LinkedHashMap::new, (m, l) -> m.put(l.getId(), l), LinkedHashMap::putAll);
        Map<Long, LockerTranslation> lockerTranslationMap = new LinkedHashMap<>();
        if (!lockerMap.isEmpty()) {
            for (Locker locker : lockerMap.values()) {
                LockerTranslation preferred = lockerTranslationRepository.findByLockerAndLanguageCode(locker, lockerLangCode).orElse(null);
                LockerTranslation fallback = lockerTranslationRepository.findByLockerAndLanguageCode(locker, "ko").orElse(null);
                if (preferred != null) lockerTranslationMap.put(locker.getId(), preferred);
                else if (fallback != null) lockerTranslationMap.put(locker.getId(), fallback);
            }
        }

        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) continue;
            for (Map<String, Object> slot : mutableSlots((Map<String, Object>) dayMap)) {
                String sourceType = str(slot.get("sourceType")).toLowerCase();
                Long sourceId = parseLongOrNull(str(slot.get("sourceId")));
                if (sourceId == null) continue;

                if (sourceType.contains("attraction")) {
                    Attraction attraction = attractionMap.get(sourceId);
                    if (attraction == null) continue;
                    AttractionTranslation tr = attractionTranslationMap.get(sourceId);
                    String name = tr != null ? str(tr.getName()) : str(attraction.getName());
                    String address = tr != null ? str(tr.getAddress()) : str(attraction.getAddress());
                    String thumbnail = str(attraction.getThumbnail());
                    if (!name.isBlank()) slot.put("placeName", name);
                    if (!address.isBlank()) slot.put("address", address);
                    if (!thumbnail.isBlank()) slot.put("thumbnail", thumbnail);
                    continue;
                }

                if (sourceType.contains("event")) {
                    TourApiEvent event = eventMap.get(sourceId);
                    if (event == null) continue;
                    TourApiEventTranslation tr = eventTranslationMap.get(sourceId);
                    String name = tr != null ? str(tr.getTitle()) : "";
                    String address = tr != null ? firstNonBlank(tr.getAddress(), tr.getEventPlace()) : "";
                    String thumbnail = firstNonBlank(event.getFirstImage2(), event.getFirstImage());
                    if (!name.isBlank()) slot.put("placeName", name);
                    if (!address.isBlank()) slot.put("address", address);
                    if (!thumbnail.isBlank()) slot.put("thumbnail", thumbnail);
                    continue;
                }

                if (sourceType.contains("locker")) {
                    LockerTranslation tr = lockerTranslationMap.get(sourceId);
                    if (tr == null) continue;
                    String name = firstNonBlank(tr.getStationName(), tr.getLockerName());
                    String address = str(tr.getDetailLocation());
                    if (!name.isBlank()) slot.put("placeName", name);
                    if (!address.isBlank()) slot.put("address", address);
                }
            }
        }
    }

    private String resolveAttractionLang(String language) {
        if (language == null || language.isBlank()) return "ko";
        String lower = language.toLowerCase();
        if (lower.startsWith("en")) return "en";
        if (lower.startsWith("ja")) return "ja";
        if (lower.startsWith("zh")) return "zh";
        return "ko";
    }

    private TourLanguage resolveTourLanguage(String language) {
        try {
            return TourLanguage.fromCode(language);
        } catch (Exception ignored) {
            return TourLanguage.KOR;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
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
                if (!str(slot.get("placeName")).isBlank() && !str(slot.get("type")).isBlank()) {
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

    /**
     * rag_documents.content에 붙는 분류 태그(중분류·소분류·대분류 괄호 블록)를 제거해 LLM 프롬프트 길이를 줄인다.
     */
    private String slimRagContentForPrompt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String s = content;
        for (int pass = 0; pass < 6; pass++) {
            String next = s
                    .replaceAll("(?U)\\[\\s*중분류\\s*\\]\\s*[^\\n\\r\\[]*", "")
                    .replaceAll("(?U)\\[\\s*소분류\\s*\\]\\s*[^\\n\\r\\[]*", "")
                    .replaceAll("(?U)\\[\\s*대분류\\s*\\]\\s*[^\\n\\r\\[]*", "");
            if (next.equals(s)) {
                break;
            }
            s = next;
        }
        return s.replaceAll("\\s{2,}", " ").trim();
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
                  "answer": "Short Markdown for the chat bubble (2-5 sentences). MUST be written entirely in the same human language as Requested language above.",
                  "days": [
                    {
                      "date": "YYYY-MM-DD",
                      "label": "1일차",
                      "slots": [
                        { "type": "...", "label": "...", "placeName": "...", "address": "...", "thumbnail": "...", "reason": "..." }
                      ]
                    }
                  ],
                  "budget": { "perPerson": "...", "total": "...", "note": "..." },
                  "summary": { "title": "...", "route": ["..."] }
                }

                Rules:
                - "answer" is required every time: friendly summary of the plan for the user, same language as Requested language (ko/en/ja/zh).
                - Each day's "label" (e.g. day title) should also be written in the same human language as Requested language (e.g. English: "Day 1", Japanese: "1日目", Korean: "1일차").
                - Slots per day are FLEXIBLE (not always 6): use the user's flight arrival time and departure time (from the trip summary / chat, e.g. lines with 도착/arrive and 출발/depart) to decide which parts of the day are realistic.
                  - First day: if arrival is late morning or afternoon, omit 아침 and possibly 오전 코스; if arrival is evening, keep only 저녁 and/or 밤 코스 as appropriate.
                  - Last day: if departure is morning or before lunch, omit 밤 코스 and 저녁 (and 오후 코스 if needed); if departure is early afternoon, trim evening slots accordingly.
                  - Middle days: usually fill as fully as reasonable (often up to the full sequence below), still respecting travel time between slots.
                - When you include multiple slots in one day, list them ONLY in this canonical order (subset allowed, never reorder): 아침 → 오전 코스 → 점심 → 오후 코스 → 저녁 → 밤 코스.
                - Prefer to pack the day densely when times allow: meal slots (아침, 점심, 저녁) with restaurant-like CANDIDATES; sightseeing slots (오전 코스, 오후 코스, 밤 코스) with attraction/culture-like CANDIDATES.
                - slot.type/label: one of 아침|오전 코스|점심|오후 코스|저녁|밤 코스 (keep these Korean tokens exactly for slot.type and slot.label so downstream parsers match)
                - slot.label == slot.type
                - slot.placeName: use CANDIDATES[].n first, never empty
                - Do not repeat the same placeName across all days/slots (case-insensitive). One place can appear only once.
                - slot.address: use CANDIDATES[].a, empty if unknown
                - slot.thumbnail: use CANDIDATES[].img if available, otherwise ""
                - slot.reason: always use empty string "" (no narrative; place detail is shown elsewhere in the app).
                - Prefer restaurant-like candidates for 아침, 점심, and 저녁 slots, but use non-restaurant fallback when restaurant candidates are insufficient.
                - Prefer attraction/culture-like candidates for 오전 코스, 오후 코스, and 밤 코스 slots, but keep fallback flexible.
                - Prefer places likely open at the slot time (morning/lunch/afternoon/dinner/night).
                - Keep consecutive moves practical; avoid transitions that are likely over 45 minutes when alternatives exist.
                - Across days: the LAST slot of day N and the FIRST slot of day N+1 should also be within ~45 minutes travel when both have locations, unless the user clearly wants a long-distance jump (e.g. new city day).
                - Never put locker/luggage-storage POIs in slots. Lockers are not loaded into CANDIDATES for this flow; the app uses a separate nearest-locker API (first-day + last-day anchors only).
                - CANDIDATES key: n=name, a=address, img=thumbnailUrl, c=category, h=operatingHours, st=sourceType, sid=sourceId, lat=latitude, lng=longitude, ls=localScore (0~1, higher = more local vibe)

                Itinerary revision (when chat history exists or the user asks to change the plan):
                - The LAST user message is the primary instruction for THIS response. Follow it over older turns if they conflict.
                - Treat as a revision when the user asks to change, replace, add, remove, swap, reorder, shorten, extend,
                  or uses Korean like 바꿔, 수정, 추가, 빼, 대신, 다른, 순서, 하루만, N일차, 점심만, 실내, 야경, 가까운, etc.
                - Start from the current plan implied by prior messages (especially the latest assistant structured itinerary).
                  Preserve day count, dates, labels, and slot order unless the user explicitly asks to change them.
                - Apply minimal edits: change only the slots or days the user targets; keep other slots identical
                  (same placeName, address, reason "") when they still satisfy constraints.
                - If the user names a place to remove, remove it and fill that slot from CANDIDATES without duplicating placeName elsewhere.
                - If the user names a theme or style (e.g. 실내, 야경, 한식만), retune affected slots to match; use CANDIDATES and PLACES.
                - If the user contradicts an earlier request, obey the latest user message.
                - summary.title, summary.route lines, and any natural-language fields in "budget" must be written in the same human language as Requested language (not necessarily Korean).
                """.formatted(language);
    }

    private String createStrictRetryPrompt() {
        return """
                Retry mode: your previous JSON had too many empty fields.
                Fill every slot with non-empty placeName (and type/label).
                Mandatory rules:
                - Include a non-empty top-level "answer" string (Markdown, user's language).
                - Each day MUST have a non-empty "slots" array. Slot count is flexible by flight arrival/departure (see main rules); within a day, slot types must appear only in canonical order (아침 → 오전 코스 → 점심 → 오후 코스 → 저녁 → 밤 코스), each at most once per day.
                - Each slot.type must be one of: 아침, 오전 코스, 점심, 오후 코스, 저녁, 밤 코스
                - Each slot.label must equal slot.type
                - slot.placeName must be a concrete place name in Seoul, never empty
                - Never duplicate placeName across days/slots (case-insensitive)
                - slot.reason must be empty string "" for every slot
                - slot.address should be filled when context contains it, otherwise use ""
                - Never output placeholder words or gibberish
                - Keep strict valid JSON only
                - If the user message is a revision: output the FULL updated days[] again; do not return partial JSON;
                  preserve unchanged slots verbatim where possible.
                """;
    }

    private String slotTypeByIndex(int index) {
        if (index < 0 || index >= SLOT_ORDER.size()) {
            return "오후 코스";
        }
        return SLOT_ORDER.get(index);
    }

    private JsonNode findNextUnusedCandidate(ArrayNode candidates, Set<String> usedPlaceNames, int seedIndex) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (int i = 0; i < candidates.size(); i++) {
            JsonNode candidate = candidates.get((seedIndex + i) % candidates.size());
            String name = candidate.path("n").asText("").trim().toLowerCase();
            if (!name.isBlank() && !usedPlaceNames.contains(name)) {
                return candidate;
            }
        }
        return null;
    }

    private JsonNode findPreferredCandidate(
            ArrayNode candidates,
            Set<String> usedPlaceNames,
            int seedIndex,
            boolean preferRestaurant,
            boolean preferAttractionLike,
            String slotType,
            String slotLabel
    ) {
        JsonNode preferred = null;
        JsonNode unusedFallback = null;
        JsonNode openFallback = null;
        for (int i = 0; i < candidates.size(); i++) {
            JsonNode candidate = candidates.get((seedIndex + i) % candidates.size());
            String name = candidate.path("n").asText("").trim().toLowerCase();
            if (name.isBlank() || usedPlaceNames.contains(name)) continue;
            if (unusedFallback == null) unusedFallback = candidate;
            if (openFallback == null && isLikelyOpenForSlot(candidate, slotType, slotLabel)) {
                openFallback = candidate;
            }
            String category = candidate.path("c").asText("");
            if (preferRestaurant && isRestaurantCategory(category) && isLikelyOpenForSlot(candidate, slotType, slotLabel)) {
                return candidate;
            }
            if (preferAttractionLike && preferred == null
                    && isAttractionLikeCategory(category)
                    && isLikelyOpenForSlot(candidate, slotType, slotLabel)) {
                preferred = candidate;
            }
        }
        if (preferred != null) return preferred;
        if (openFallback != null) return openFallback;
        return unusedFallback;
    }

    private boolean isMealSlot(String type, String label) {
        String text = (nullToEmpty(type) + " " + nullToEmpty(label)).toLowerCase();
        return text.contains("아침")
                || text.contains("점심")
                || text.contains("저녁")
                || text.contains("breakfast")
                || text.contains("lunch")
                || text.contains("dinner");
    }

    @SuppressWarnings("unchecked")
    private void clearAllSlotReasons(List<?> dayList) {
        if (dayList == null) {
            return;
        }
        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) {
                continue;
            }
            for (Map<String, Object> slot : mutableSlots((Map<String, Object>) dayMap)) {
                slot.put("reason", "");
            }
        }
    }

    private boolean isRestaurantCategory(String category) {
        String c = nullToEmpty(category).toLowerCase();
        return c.contains("restaurant")
                || c.contains("food")
                || c.contains("맛집")
                || c.contains("음식")
                || c.contains("카페");
    }

    private boolean isAttractionLikeCategory(String category) {
        String c = nullToEmpty(category).toLowerCase();
        return c.contains("attraction")
                || c.contains("event")
                || c.contains("shopping")
                || c.contains("night")
                || c.contains("문화")
                || c.contains("관광")
                || c.contains("전시")
                || c.contains("체험");
    }

    private boolean isLikelyOpenForSlot(JsonNode candidate, String slotType, String slotLabel) {
        String hours = candidate.path("h").asText("");
        if (hours == null || hours.isBlank()) return true;
        String normalized = hours.toLowerCase();
        if (normalized.contains("24시간") || normalized.contains("24h")) return true;
        if (normalized.contains("휴무")) return false;

        Matcher matcher = TIME_RANGE_PATTERN.matcher(normalized);
        if (!matcher.find()) return true;

        int startHour = parseHour(matcher.group(1), matcher.group(2));
        int endHour = parseHour(matcher.group(3), matcher.group(4));
        if (startHour < 0 || endHour < 0) return true;

        int targetHour = targetHourBySlot(slotType, slotLabel);
        if (targetHour < 0) return true;

        if (endHour < startHour) {
            return targetHour >= startHour || targetHour <= endHour;
        }
        return targetHour >= startHour && targetHour <= endHour;
    }

    private int parseHour(String hourText, String minText) {
        try {
            int hour = Integer.parseInt(hourText);
            int min = (minText == null || minText.isBlank()) ? 0 : Integer.parseInt(minText);
            if (hour < 0 || hour > 24) return -1;
            if (min < 0 || min > 59) return -1;
            return hour;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private int targetHourBySlot(String type, String label) {
        String text = (nullToEmpty(type) + " " + nullToEmpty(label)).toLowerCase();
        if (text.contains("아침") || text.contains("breakfast")) return 8;
        if (text.contains("오전") || text.contains("morning")) return 10;
        if (text.contains("점심") || text.contains("lunch")) return 13;
        if (text.contains("오후") || text.contains("afternoon")) return 16;
        if (text.contains("저녁") || text.contains("dinner")) return 19;
        if (text.contains("밤") || text.contains("night")) return 21;
        return -1;
    }

    @SuppressWarnings("unchecked")
    /** 코스 생성 직후 메시지가 짧아도, 이전 user 턴 요약에 비행 시각이 있을 수 있음 */
    private String userTextBlobForFlightParsing(AiChatRequest request) {
        if (request == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (request.getHistory() != null) {
            for (AiChatRequest.ChatHistoryMessage m : request.getHistory()) {
                if (m == null || m.getRole() == null || m.getContent() == null) {
                    continue;
                }
                if ("user".equalsIgnoreCase(m.getRole().trim())) {
                    sb.append('\n').append(m.getContent());
                }
            }
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            sb.append('\n').append(request.getMessage());
        }
        return sb.toString();
    }

    private void applyFlightTimeWindow(List<?> dayList, AiChatRequest request) {
        if (dayList == null || dayList.isEmpty() || request == null) return;
        String blob = userTextBlobForFlightParsing(request);
        Integer arrivalHour = extractHour(blob, ARRIVAL_TIME_PATTERN);
        Integer departureHour = extractHour(blob, DEPARTURE_TIME_PATTERN);

        if (arrivalHour != null) {
            Map<String, Object> firstDay = firstMap(dayList);
            clampDaySlotsByHour(firstDay, arrivalHour, true);
        }
        if (departureHour != null) {
            Map<String, Object> lastDay = lastMap(dayList);
            clampDaySlotsByHour(lastDay, departureHour, false);
        }
    }

    private Integer extractHour(String message, Pattern pattern) {
        if (message == null || message.isBlank()) return null;
        Matcher m = pattern.matcher(message);
        if (!m.find()) return null;
        String hhmm = m.group(1);
        String[] parts = hhmm.split(":");
        if (parts.length != 2) return null;
        try {
            int hour = Integer.parseInt(parts[0]);
            return (hour >= 0 && hour <= 23) ? hour : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void clampDaySlotsByHour(Map<String, Object> day, int hour, boolean isArrival) {
        if (day == null) return;
        Object slotsObj = day.get("slots");
        if (!(slotsObj instanceof List<?> slotList) || slotList.isEmpty()) return;

        List<Map<String, Object>> kept = new ArrayList<>();
        Map<String, Object> fallback = null;
        int bestDiff = Integer.MAX_VALUE;

        for (Object slotRaw : slotList) {
            if (!(slotRaw instanceof Map<?, ?> slotMap)) continue;
            Map<String, Object> slot = (Map<String, Object>) slotMap;
            int slotHour = targetHourBySlot(str(slot.get("type")), str(slot.get("label")));
            if (slotHour < 0) {
                kept.add(slot);
                continue;
            }

            boolean inRange = isArrival ? slotHour >= hour : slotHour <= hour;
            if (inRange) {
                kept.add(slot);
            }

            int diff = Math.abs(slotHour - hour);
            if (diff < bestDiff) {
                bestDiff = diff;
                fallback = slot;
            }
        }

        if (kept.isEmpty() && fallback != null) {
            kept.add(fallback);
        }
        if (!kept.isEmpty()) {
            day.put("slots", kept);
        }
    }

    private JsonNode findCandidateByPlaceName(ArrayNode candidates, String placeName) {
        if (candidates == null || candidates.isEmpty() || placeName == null || placeName.isBlank()) {
            return null;
        }
        String normalized = placeName.trim().toLowerCase();
        JsonNode fallback = null;
        for (JsonNode candidate : candidates) {
            String candidateName = candidate.path("n").asText("").trim().toLowerCase();
            if (candidateName.isBlank()) continue;
            if (candidateName.equals(normalized)) return candidate;
            if (fallback == null && (candidateName.contains(normalized) || normalized.contains(candidateName))) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private void putSlotCoordinates(Map<String, Object> slot, JsonNode candidate) {
        if (slot == null || candidate == null) return;
        if (!candidate.path("lat").isMissingNode() && !candidate.path("lat").isNull()) {
            slot.put("lat", candidate.path("lat").asDouble());
        }
        if (!candidate.path("lng").isMissingNode() && !candidate.path("lng").isNull()) {
            slot.put("lng", candidate.path("lng").asDouble());
        }
    }

    @SuppressWarnings("unchecked")
    private void putSlotSourceInfo(Map<String, Object> slot, JsonNode candidate) {
        if (slot == null || candidate == null) return;
        String st = candidate.path("st").asText("");
        String sid = candidate.path("sid").asText("");
        String thumbnail = candidate.path("img").asText("");
        if (!st.isBlank()) slot.put("sourceType", st);
        if (!sid.isBlank()) slot.put("sourceId", sid);
        if (!thumbnail.isBlank()) slot.put("thumbnail", thumbnail);
        String cat = candidate.path("c").asText("");
        if (!cat.isBlank()) {
            slot.put("category", cat);
        }
        if (!candidate.path("ls").isMissingNode() && !candidate.path("ls").isNull()) {
            slot.put("localScore", candidate.path("ls").asDouble());
        }
    }

    @SuppressWarnings("unchecked")
    private void optimizeDailyTravel(List<?> dayList, ArrayNode candidates) {
        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) continue;
            Object slotsObj = ((Map<String, Object>) dayMap).get("slots");
            if (!(slotsObj instanceof List<?> slotList)) continue;
            List<Map<String, Object>> slots = new ArrayList<>();
            for (Object slotRaw : slotList) {
                if (slotRaw instanceof Map<?, ?> slotMap) {
                    slots.add((Map<String, Object>) slotMap);
                }
            }
            if (slots.size() < 2) continue;
            rebalanceLongMoves(slots, candidates);
        }
    }

    /**
     * 전날 마지막 슬롯과 다음날 첫 슬롯 사이 이동이 과도하면, 다음날 첫 코스를 더 가까운 후보로 교체한다.
     * (물품보관함 전용 슬롯이 있으면 앵커/첫칸으로 쓰지 않음)
     */
    @SuppressWarnings("unchecked")
    private void optimizeCrossDayTravel(List<?> dayList, ArrayNode candidates) {
        if (dayList == null || dayList.size() < 2 || candidates == null || candidates.isEmpty()) {
            return;
        }
        Set<String> usedNames = collectAllPlaceNames(dayList);
        for (int d = 0; d < dayList.size() - 1; d++) {
            Object dayARaw = dayList.get(d);
            Object dayBRaw = dayList.get(d + 1);
            if (!(dayARaw instanceof Map<?, ?> dayAMap) || !(dayBRaw instanceof Map<?, ?> dayBMap)) {
                continue;
            }
            List<Map<String, Object>> slotsA = mutableSlots((Map<String, Object>) dayAMap);
            List<Map<String, Object>> slotsB = mutableSlots((Map<String, Object>) dayBMap);
            if (slotsA.isEmpty() || slotsB.isEmpty()) {
                continue;
            }
            Map<String, Object> lastPrev = slotsA.get(slotsA.size() - 1);
            Map<String, Object> firstNext = slotsB.get(0);
            if (isLockerSlot(lastPrev) || isLockerSlot(firstNext)) {
                continue;
            }
            Double anchorLat = asDouble(lastPrev.get("lat"));
            Double anchorLng = asDouble(lastPrev.get("lng"));
            Double fstLat = asDouble(firstNext.get("lat"));
            Double fstLng = asDouble(firstNext.get("lng"));
            if (anchorLat == null || anchorLng == null || fstLat == null || fstLng == null) {
                continue;
            }
            int minutes = estimateTravelMinutes(anchorLat, anchorLng, fstLat, fstLng);
            if (minutes <= MAX_TRAVEL_MINUTES) {
                continue;
            }
            boolean mealFirst = isMealSlot(str(firstNext.get("type")), str(firstNext.get("label")));
            JsonNode close = findCloserCandidate(
                    candidates,
                    usedNames,
                    anchorLat,
                    anchorLng,
                    mealFirst,
                    !mealFirst,
                    str(firstNext.get("type")),
                    str(firstNext.get("label"))
            );
            if (close == null) {
                continue;
            }
            String oldName = str(firstNext.get("placeName")).toLowerCase();
            if (!oldName.isBlank()) {
                usedNames.remove(oldName);
            }
            String newName = close.path("n").asText("").trim();
            firstNext.put("placeName", newName);
            firstNext.put("address", close.path("a").asText(""));
            putSlotCoordinates(firstNext, close);
            putSlotSourceInfo(firstNext, close);
            firstNext.put("reason", "");
            if (!newName.isBlank()) {
                usedNames.add(newName.toLowerCase());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> collectAllPlaceNames(List<?> dayList) {
        Set<String> used = new HashSet<>();
        for (Object dayRaw : dayList) {
            if (!(dayRaw instanceof Map<?, ?> dayMap)) {
                continue;
            }
            for (Map<String, Object> slot : mutableSlots((Map<String, Object>) dayMap)) {
                String n = str(slot.get("placeName")).toLowerCase();
                if (!n.isBlank()) {
                    used.add(n);
                }
            }
        }
        return used;
    }

    private boolean isLockerSlot(Map<String, Object> slot) {
        if (slot == null) {
            return false;
        }
        String st = str(slot.get("sourceType")).toLowerCase();
        String cat = str(slot.get("category")).toLowerCase();
        return st.contains("locker") || cat.contains("locker");
    }

    private void rebalanceLongMoves(List<Map<String, Object>> slots, ArrayNode candidates) {
        Set<String> usedNames = new HashSet<>();
        for (Map<String, Object> slot : slots) {
            String n = str(slot.get("placeName")).toLowerCase();
            if (!n.isBlank()) usedNames.add(n);
        }

        for (int i = 1; i < slots.size(); i++) {
            Map<String, Object> prev = slots.get(i - 1);
            Map<String, Object> cur = slots.get(i);
            Double prevLat = asDouble(prev.get("lat"));
            Double prevLng = asDouble(prev.get("lng"));
            Double curLat = asDouble(cur.get("lat"));
            Double curLng = asDouble(cur.get("lng"));
            if (prevLat == null || prevLng == null || curLat == null || curLng == null) continue;

            int minutes = estimateTravelMinutes(prevLat, prevLng, curLat, curLng);
            if (minutes <= MAX_TRAVEL_MINUTES) continue;

            boolean mealSlot = isMealSlot(str(cur.get("type")), str(cur.get("label")));
            JsonNode closeCandidate = findCloserCandidate(
                    candidates,
                    usedNames,
                    prevLat,
                    prevLng,
                    mealSlot,
                    !mealSlot,
                    str(cur.get("type")),
                    str(cur.get("label"))
            );
            if (closeCandidate == null) continue;

            String oldName = str(cur.get("placeName")).toLowerCase();
            if (!oldName.isBlank()) usedNames.remove(oldName);
            String newName = closeCandidate.path("n").asText("").trim();
            cur.put("placeName", newName);
            cur.put("address", closeCandidate.path("a").asText(""));
            putSlotCoordinates(cur, closeCandidate);
            putSlotSourceInfo(cur, closeCandidate);
            cur.put("reason", "");
            if (!newName.isBlank()) usedNames.add(newName.toLowerCase());
        }
    }

    private JsonNode findCloserCandidate(
            ArrayNode candidates,
            Set<String> usedNames,
            double fromLat,
            double fromLng,
            boolean preferRestaurant,
            boolean preferAttractionLike,
            String slotType,
            String slotLabel
    ) {
        JsonNode best = null;
        int bestMinutes = Integer.MAX_VALUE;
        for (JsonNode candidate : candidates) {
            String name = candidate.path("n").asText("").trim().toLowerCase();
            if (name.isBlank() || usedNames.contains(name)) continue;
            Double lat = asDouble(candidate.path("lat").isMissingNode() ? null : candidate.path("lat").asDouble());
            Double lng = asDouble(candidate.path("lng").isMissingNode() ? null : candidate.path("lng").asDouble());
            if (lat == null || lng == null) continue;
            if (!isLikelyOpenForSlot(candidate, slotType, slotLabel)) continue;
            String category = candidate.path("c").asText("");
            if (preferRestaurant && !isRestaurantCategory(category)) continue;
            if (preferAttractionLike && !isAttractionLikeCategory(category)) continue;
            int minutes = estimateTravelMinutes(fromLat, fromLng, lat, lng);
            if (minutes < bestMinutes) {
                bestMinutes = minutes;
                best = candidate;
            }
        }
        if (best != null && bestMinutes <= MAX_TRAVEL_MINUTES) return best;
        return null;
    }

    private int estimateTravelMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        double km = haversineKm(fromLat, fromLng, toLat, toLng);
        return (int) Math.round((km / CITY_SPEED_KMH) * 60.0);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private Double asDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstMap(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        Object raw = list.get(0);
        if (raw instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lastMap(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        Object raw = list.get(list.size() - 1);
        if (raw instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mutableSlots(Map<String, Object> day) {
        if (day == null) return List.of();
        Object slotsObj = day.get("slots");
        if (!(slotsObj instanceof List<?> slotList)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object slotRaw : slotList) {
            if (slotRaw instanceof Map<?, ?> slotMap) {
                out.add((Map<String, Object>) slotMap);
            }
        }
        return out;
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

    private record TripDateRange(LocalDate start, LocalDate end) {
    }
}
