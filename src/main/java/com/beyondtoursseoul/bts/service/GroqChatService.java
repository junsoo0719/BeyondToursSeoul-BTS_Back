package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.course.CourseItemType;
import com.beyondtoursseoul.bts.domain.course.TourCourse;
import com.beyondtoursseoul.bts.domain.course.TourCourseItem;
import com.beyondtoursseoul.bts.domain.course.UserSavedCourse;
import com.beyondtoursseoul.bts.domain.locker.Locker;
import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import com.beyondtoursseoul.bts.domain.saved.UserSavedAttraction;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.AiChatRequest;
import com.beyondtoursseoul.bts.dto.AiChatResponse;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.AttractionTranslationRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseItemRepository;
import com.beyondtoursseoul.bts.repository.course.UserSavedCourseRepository;
import com.beyondtoursseoul.bts.repository.locker.LockerRepository;
import com.beyondtoursseoul.bts.repository.locker.LockerTranslationRepository;
import com.beyondtoursseoul.bts.repository.saved.UserSavedAttractionRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroqChatService {
    private static final int CANDIDATE_LIMIT_MIN = 12;
    private static final int CANDIDATE_LIMIT_MAX = 56;
    /** 저장 반영: 요청 ID·프롬프트 줄/필드/전체 글자 상한 (컨텍스트 비대화). */
    private static final int USER_SAVED_REQUEST_ID_CAP = 24;
    private static final int USER_SAVED_PROMPT_MAX_ATTR_ROWS = 10;
    private static final int USER_SAVED_PROMPT_MAX_COURSE_ROWS = 6;
    private static final int USER_SAVED_PROMPT_FIELD_CHARS = 96;
    private static final int USER_SAVED_PROMPT_TOTAL_CHARS = 2000;
    /** 저장 공식 코스에서 CANDIDATES로 끌어올 관광지 스팟 상한(코스당 순서 유지·중복 제거) */
    private static final int SAVED_COURSE_SPOT_ATTRACTION_CAP = 20;
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
    private final ProfileRepository profileRepository;
    private final UserSavedAttractionRepository userSavedAttractionRepository;
    private final UserSavedCourseRepository userSavedCourseRepository;
    private final TourCourseItemRepository tourCourseItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    /** 완성 토큰 상한(TPM·비용). application.yml groq.api.max-completion-tokens */
    @Value("${groq.api.max-completion-tokens:850}")
    private int maxCompletionTokens;

    /** LLM 프롬프트에 실을 CANDIDATES 최대 개수(전체 배열·후처리는 그대로) */
    @Value("${groq.api.prompt-max-candidates:34}")
    private int promptMaxCandidates;

    @Value("${groq.api.prompt-max-history-messages:6}")
    private int promptMaxHistoryMessages;

    @Value("${groq.api.prompt-max-history-chars:380}")
    private int promptMaxHistoryChars;

    @Value("${groq.api.prompt-max-user-chars:2600}")
    private int promptMaxUserChars;

    @Value("${groq.api.prompt-max-places:4}")
    private int promptMaxPlaces;

    @Value("${groq.api.prompt-max-place-info-chars:48}")
    private int promptMaxPlaceInfoChars;

    /** RAG 검색 후 코스 후보로 쓸 문서 최대 개수(단일 Groq 호출용 입력 절약) */
    @Value("${groq.api.rag-document-cap:22}")
    private int ragDocumentCap;

    public GroqChatService(
            RagSearchService ragSearchService,
            AttractionRepository attractionRepository,
            AttractionTranslationRepository attractionTranslationRepository,
            TourApiEventRepository tourApiEventRepository,
            TourApiEventTranslationRepository tourApiEventTranslationRepository,
            LockerRepository lockerRepository,
            LockerTranslationRepository lockerTranslationRepository,
            ProfileRepository profileRepository,
            UserSavedAttractionRepository userSavedAttractionRepository,
            UserSavedCourseRepository userSavedCourseRepository,
            TourCourseItemRepository tourCourseItemRepository,
            @Value("${groq.api.base-url:https://api.groq.com/openai/v1}") String baseUrl
    ) {
        this.ragSearchService = ragSearchService;
        this.attractionRepository = attractionRepository;
        this.attractionTranslationRepository = attractionTranslationRepository;
        this.tourApiEventRepository = tourApiEventRepository;
        this.tourApiEventTranslationRepository = tourApiEventTranslationRepository;
        this.lockerRepository = lockerRepository;
        this.lockerTranslationRepository = lockerTranslationRepository;
        this.profileRepository = profileRepository;
        this.userSavedAttractionRepository = userSavedAttractionRepository;
        this.userSavedCourseRepository = userSavedCourseRepository;
        this.tourCourseItemRepository = tourCourseItemRepository;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Transactional(readOnly = true)
    public AiChatResponse chat(AiChatRequest request, UUID authenticatedUserId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY가 설정되어 있지 않습니다.");
        }

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalStateException("message는 필수입니다.");
        }

        if (authenticatedUserId == null && hasNonEmptySavedSelections(request)) {
            log.debug("[AI] 저장 항목 ID가 요청에 포함되었으나 인증 사용자가 없어 무시합니다.");
        }

        List<Attraction> validatedSavedAttractions =
                resolveValidatedSavedAttractions(authenticatedUserId, request.getSavedAttractionIds());
        List<TourCourse> validatedSavedCourses =
                resolveValidatedSavedCourses(authenticatedUserId, request.getSavedCourseIds());
        List<Attraction> courseSpotAttractions = resolveAttractionsFromSavedCourses(validatedSavedCourses);
        List<Attraction> savedAttractionsForMerge =
                mergeSavedAttractionListsForCandidates(courseSpotAttractions, validatedSavedAttractions);
        String userSavedBlock = buildUserSavedSelectionsSystemContent(validatedSavedAttractions, validatedSavedCourses);

        List<RagSearchService.RagDocumentContext> ragDocs = searchRagDocuments(request, validatedSavedCourses);
        ragDocs = interleaveRestaurantAttractionForPrompt(ragDocs);
        int ragCap = Math.max(10, Math.min(ragDocumentCap, 80));
        if (ragDocs.size() > ragCap) {
            log.info("[AI] RAG 문서 {}건 → 단일 Groq 호출용 {}건으로 제한", ragDocs.size(), ragCap);
            ragDocs = new ArrayList<>(ragDocs.subList(0, ragCap));
        }
        String ragContext = createRagContext(ragDocs);
        ArrayNode candidateArray = buildCandidateArray(ragDocs, request);
        int docCandidateLimit = candidateArrayLimit(ragDocs);
        int mergeCap = Math.min(
                CANDIDATE_LIMIT_MAX,
                docCandidateLimit + Math.min(20, savedAttractionsForMerge.size())
        );
        Map<String, Boolean> dedupe = dedupePlaceNamesFromCandidates(candidateArray);
        mergeUserSavedAttractionsIntoCandidates(candidateArray, savedAttractionsForMerge, dedupe, mergeCap);
        trimCandidateArrayToMax(candidateArray, mergeCap);

        String rawContent = requestCompletion(request, model, false, ragContext, candidateArray, userSavedBlock);
        log.info("[AI] model={}, rawResponse={}", model, truncateForLog(rawContent, 3000));
        AiChatResponse parsed = parseAiChatResponse(rawContent, model);
        parsed = ensureStructuredQuality(parsed, candidateArray, request);

        if (needsRetry(parsed, request)) {
            log.warn(
                    "[AI] 첫 응답이 품질·일차 기준에 못 미치지만, 정책상 Groq는 요청당 1회만 호출합니다(추가 재생성·429 재시도 없음)."
            );
        }

        log.info(
                "[AI] parsedResponse answerLength={}, hasStructured={}",
                parsed.getAnswer() == null ? 0 : parsed.getAnswer().length(),
                parsed.getStructured() != null
        );
        return parsed;
    }

    private static boolean hasNonEmptySavedSelections(AiChatRequest request) {
        return (request.getSavedAttractionIds() != null && !request.getSavedAttractionIds().isEmpty())
                || (request.getSavedCourseIds() != null && !request.getSavedCourseIds().isEmpty());
    }

    /** 요청 순서 유지, 중복 제거, 최대 {@link #USER_SAVED_REQUEST_ID_CAP}개. */
    private static List<Long> capSavedRequestIds(List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }
        List<Long> idOrder = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : requestedIds) {
            if (id == null || seen.contains(id)) {
                continue;
            }
            seen.add(id);
            idOrder.add(id);
            if (idOrder.size() >= USER_SAVED_REQUEST_ID_CAP) {
                break;
            }
        }
        return idOrder;
    }

    private List<Attraction> resolveValidatedSavedAttractions(UUID userId, List<Long> requestedIds) {
        if (userId == null || requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }
        Profile user = profileRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        List<Long> idOrder = capSavedRequestIds(requestedIds);
        if (idOrder.isEmpty()) {
            return List.of();
        }
        Set<Long> want = new LinkedHashSet<>(idOrder);
        Map<Long, Attraction> byId = userSavedAttractionRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(UserSavedAttraction::getAttraction)
                .filter(Objects::nonNull)
                .filter(a -> want.contains(a.getId()))
                .collect(Collectors.toMap(Attraction::getId, a -> a, (a, b) -> a));
        List<Attraction> ordered = new ArrayList<>();
        for (Long id : idOrder) {
            Attraction a = byId.get(id);
            if (a != null) {
                ordered.add(a);
            }
        }
        return ordered;
    }

    private List<TourCourse> resolveValidatedSavedCourses(UUID userId, List<Long> requestedIds) {
        if (userId == null || requestedIds == null || requestedIds.isEmpty()) {
            return List.of();
        }
        Profile user = profileRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        List<Long> idOrder = capSavedRequestIds(requestedIds);
        if (idOrder.isEmpty()) {
            return List.of();
        }
        Set<Long> want = new LinkedHashSet<>(idOrder);
        Map<Long, TourCourse> byId = userSavedCourseRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(UserSavedCourse::getCourse)
                .filter(Objects::nonNull)
                .filter(c -> want.contains(c.getId()))
                .collect(Collectors.toMap(TourCourse::getId, c -> c, (a, b) -> a));
        List<TourCourse> ordered = new ArrayList<>();
        for (Long id : idOrder) {
            TourCourse c = byId.get(id);
            if (c != null) {
                ordered.add(c);
            }
        }
        return ordered;
    }

    /**
     * 저장 공식 코스에 포함된 관광지 스팟을 순서대로 모은다(이벤트만 있는 구간은 건너뜀).
     * 이 목록은 CANDIDATES 선두에 합쳐져 모델이 해시태그 문자열 대신 실제 POI 이름을 쓰도록 돕는다.
     */
    private List<Attraction> resolveAttractionsFromSavedCourses(List<TourCourse> courses) {
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream()
                .map(TourCourse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<TourCourseItem> rows = tourCourseItemRepository.findByCourseIdInWithSpotsFetchedOrdered(courseIds);
        List<Attraction> ordered = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (TourCourseItem row : rows) {
            if (ordered.size() >= SAVED_COURSE_SPOT_ATTRACTION_CAP) {
                break;
            }
            if (row.getItemType() != CourseItemType.ATTRACTION) {
                continue;
            }
            Attraction a = row.getAttraction();
            if (a == null || a.getId() == null || seen.contains(a.getId())) {
                continue;
            }
            seen.add(a.getId());
            ordered.add(a);
        }
        return ordered;
    }

    /** 코스에서 온 스팟을 먼저, 그다음 사용자가 직접 고른 저장 관광지(중복 제거). */
    private static List<Attraction> mergeSavedAttractionListsForCandidates(
            List<Attraction> courseSpotAttractions,
            List<Attraction> directSavedAttractions
    ) {
        Map<Long, Attraction> byId = new LinkedHashMap<>();
        if (courseSpotAttractions != null) {
            for (Attraction a : courseSpotAttractions) {
                if (a != null && a.getId() != null) {
                    byId.putIfAbsent(a.getId(), a);
                }
            }
        }
        if (directSavedAttractions != null) {
            for (Attraction a : directSavedAttractions) {
                if (a != null && a.getId() != null) {
                    byId.putIfAbsent(a.getId(), a);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    private String buildUserSavedSelectionsSystemContent(List<Attraction> attractions, List<TourCourse> courses) {
        if ((attractions == null || attractions.isEmpty()) && (courses == null || courses.isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("USER_SELECTED_SAVED_LIST:\n");
        sb.append("- Saved attractions: prefer similar areas/categories from CANDIDATES; at most 1-2 slot overlaps with listed names if natural.\n");
        sb.append("- Saved official courses: title and hashtags are MOOD/STYLE hints only. ");
        sb.append("Never use a hashtag string or course title as slot.placeName. ");
        sb.append("slot.placeName must always match a name from CANDIDATES[].n (official course POIs are merged into CANDIDATES when possible).\n");
        if (attractions != null && !attractions.isEmpty()) {
            sb.append("Saved attractions (name | address | category):\n");
            int rows = 0;
            for (Attraction a : attractions) {
                if (rows >= USER_SAVED_PROMPT_MAX_ATTR_ROWS) {
                    break;
                }
                sb.append("- ")
                        .append(savedPromptField(nullToEmpty(a.getName())))
                        .append(" | ")
                        .append(savedPromptField(nullToEmpty(a.getAddress())))
                        .append(" | ")
                        .append(savedPromptField(nullToEmpty(a.getCategory())))
                        .append("\n");
                rows++;
            }
        }
        if (courses != null && !courses.isEmpty()) {
            sb.append("Saved official tour course themes (title | hashtags):\n");
            int rows = 0;
            for (TourCourse c : courses) {
                if (rows >= USER_SAVED_PROMPT_MAX_COURSE_ROWS) {
                    break;
                }
                sb.append("- ")
                        .append(savedPromptField(nullToEmpty(c.getTitle())))
                        .append(" | ")
                        .append(savedPromptField(nullToEmpty(c.getHashtags())))
                        .append("\n");
                rows++;
            }
        }
        return truncate(sb.toString(), USER_SAVED_PROMPT_TOTAL_CHARS);
    }

    private String savedPromptField(String raw) {
        String t = sanitizePromptLine(nullToEmpty(raw));
        if (t.isEmpty()) {
            return "";
        }
        return truncate(t, USER_SAVED_PROMPT_FIELD_CHARS);
    }

    private static String sanitizePromptLine(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static Map<String, Boolean> dedupePlaceNamesFromCandidates(ArrayNode array) {
        Map<String, Boolean> dedupe = new LinkedHashMap<>();
        if (array == null) {
            return dedupe;
        }
        for (JsonNode n : array) {
            String key = n.path("n").asText("").trim().toLowerCase();
            if (!key.isBlank()) {
                dedupe.put(key, true);
            }
        }
        return dedupe;
    }

    private void mergeUserSavedAttractionsIntoCandidates(
            ArrayNode array,
            List<Attraction> savedAttractions,
            Map<String, Boolean> dedupe,
            int maxTotal
    ) {
        if (array == null || savedAttractions == null || savedAttractions.isEmpty()) {
            return;
        }
        for (int i = savedAttractions.size() - 1; i >= 0; i--) {
            if (array.size() >= maxTotal) {
                break;
            }
            Attraction att = savedAttractions.get(i);
            if (att == null) {
                continue;
            }
            String name = nullToEmpty(att.getName()).trim();
            if (name.isBlank()) {
                continue;
            }
            String key = name.toLowerCase();
            if (dedupe.containsKey(key)) {
                continue;
            }
            dedupe.put(key, true);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("n", name);
            node.put("a", nullToEmpty(att.getAddress()));
            node.put("c", nullToEmpty(att.getCategory()));
            node.put("h", nullToEmpty(att.getOperatingHours()));
            node.put("st", "attraction");
            node.put("sid", String.valueOf(att.getId()));
            if (att.getThumbnail() != null && !att.getThumbnail().isBlank()) {
                node.put("img", att.getThumbnail());
            }
            if (att.getGeom() != null) {
                node.put("lat", att.getGeom().getY());
                node.put("lng", att.getGeom().getX());
            }
            array.insert(0, node);
        }
    }

    private static void trimCandidateArrayToMax(ArrayNode array, int max) {
        if (array == null || max <= 0) {
            return;
        }
        while (array.size() > max) {
            array.remove(array.size() - 1);
        }
    }

    /**
     * RAG diversify 결과가 보통 [식당…][관광…] 순인데, 이후 {@code subList(0, ragCap)}만 하면
     * 앞쪽이 식당으로만 잘리는 경우가 있어 식당·관광을 한 건씩 교차해 Groq 입력 비중을 맞춘다.
     */
    private static List<RagSearchService.RagDocumentContext> interleaveRestaurantAttractionForPrompt(
            List<RagSearchService.RagDocumentContext> docs) {
        if (docs == null || docs.isEmpty()) {
            return docs == null ? List.of() : docs;
        }
        List<RagSearchService.RagDocumentContext> restaurants = new ArrayList<>();
        List<RagSearchService.RagDocumentContext> attractions = new ArrayList<>();
        List<RagSearchService.RagDocumentContext> others = new ArrayList<>();
        for (RagSearchService.RagDocumentContext d : docs) {
            String c = d.category();
            if ("restaurant".equalsIgnoreCase(c)) {
                restaurants.add(d);
            } else if ("attraction".equalsIgnoreCase(c)) {
                attractions.add(d);
            } else {
                others.add(d);
            }
        }
        if (restaurants.isEmpty() || attractions.isEmpty()) {
            return new ArrayList<>(docs);
        }
        List<RagSearchService.RagDocumentContext> out = new ArrayList<>(docs.size());
        int ri = 0;
        int ai = 0;
        while (ri < restaurants.size() || ai < attractions.size()) {
            if (ri < restaurants.size()) {
                out.add(restaurants.get(ri++));
            }
            if (ai < attractions.size()) {
                out.add(attractions.get(ai++));
            }
        }
        out.addAll(others);
        return out;
    }

    private List<RagSearchService.RagDocumentContext> searchRagDocuments(
            AiChatRequest request,
            List<TourCourse> savedCoursesForKeywords
    ) {
        String searchQuery = createRagSearchQuery(request, savedCoursesForKeywords);
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

    /** Groq 는 HTTP 요청당 1회만 호출(429/TPM 재시도 없음, needsRetry 2차 호출 없음). */
    private String requestCompletion(
            AiChatRequest request,
            String modelName,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray,
            String userSavedBlock
    ) {
        return executeGroqChatCompletion(
                request, modelName, strictMode, ragContext, candidateArray, userSavedBlock
        );
    }

    private String executeGroqChatCompletion(
            AiChatRequest request,
            String modelName,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray,
            String userSavedBlock
    ) {
        GroqChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(createRequestBody(request, modelName, strictMode, ragContext, candidateArray, userSavedBlock))
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
            ArrayNode candidateArray,
            String userSavedBlock
    ) {
        String language = request.getLanguage() == null || request.getLanguage().isBlank()
                ? "ko"
                : request.getLanguage();

        int cappedMaxTokens = Math.max(400, Math.min(2048, maxCompletionTokens));
        return Map.of(
                "model", modelName,
                "temperature", strictMode ? 0.0 : 0.2,
                "max_tokens", cappedMaxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", createMessages(request, language, strictMode, ragContext, candidateArray, userSavedBlock)
        );
    }

    private List<Map<String, String>> createMessages(
            AiChatRequest request,
            String language,
            boolean strictMode,
            String ragContext,
            ArrayNode candidateArray,
            String userSavedBlock
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

        String tripDayConstraint = buildTripCalendarDayConstraintSystemContent(request);
        if (tripDayConstraint != null && !tripDayConstraint.isBlank()) {
            messages.add(Map.of("role", "system", "content", tripDayConstraint));
        }

        if (!ragContext.isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", ragContext
            ));
        }
        if (userSavedBlock != null && !userSavedBlock.isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", userSavedBlock
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
            int historyLimit = Math.max(2, Math.min(12, promptMaxHistoryMessages));
            int historyChars = Math.max(200, Math.min(800, promptMaxHistoryChars));
            int skip = Math.max(0, validHistory.size() - historyLimit);
            validHistory.stream()
                    .skip(skip)
                    .map(history -> Map.of(
                            "role", history.getRole().trim().toLowerCase(),
                            "content", truncate(history.getContent(), historyChars)
                    ))
                    .forEach(messages::add);
        }

        int userChars = Math.max(800, Math.min(8000, promptMaxUserChars));
        messages.add(Map.of(
                "role", "user",
                "content", truncate(request.getMessage(), userChars)
        ));

        return messages;
    }

    // RAG context: 상위 N개, 제목+짧은 요약만 (TPM 절약)
    private String createRagContext(List<RagSearchService.RagDocumentContext> documents) {
        if (documents.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("PLACES:[");
        int limit = Math.min(documents.size(), Math.max(3, Math.min(12, promptMaxPlaces)));
        int infoChars = Math.max(32, Math.min(120, promptMaxPlaceInfoChars));
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
                            infoChars))
                    .append("\"")
                    .append("}");
        }
        context.append("]");
        return context.toString();
    }

    // CANDIDATES: LLM용은 앞쪽 일부 + 짧은 필드만(전체 candidateArray 는 후처리용으로 유지). img URL 은 토큰 폭발 방지로 생략.
    private String createCandidatePrompt(ArrayNode candidates) {
        int cap = Math.max(8, Math.min(promptMaxCandidates, 120));
        ArrayNode promptCandidates = objectMapper.createArrayNode();
        int n = Math.min(candidates == null ? 0 : candidates.size(), cap);
        if (candidates != null && candidates.size() > cap) {
            log.debug("[AI] CANDIDATES prompt cap: {} -> {}", candidates.size(), cap);
        }
        for (int i = 0; i < n; i++) {
            JsonNode candidate = candidates.get(i);
            ObjectNode node = promptCandidates.addObject();
            node.put("n", truncate(nullToEmpty(candidate.path("n").asText("")).trim(), 56));
            node.put("a", truncate(nullToEmpty(candidate.path("a").asText("")).trim(), 72));
            node.put("c", truncate(nullToEmpty(candidate.path("c").asText("")).trim(), 28));
            node.put("h", truncate(nullToEmpty(candidate.path("h").asText("")).trim(), 40));
            node.put("st", truncate(nullToEmpty(candidate.path("st").asText("")).trim(), 24));
            node.put("sid", truncate(nullToEmpty(candidate.path("sid").asText("")).trim(), 20));
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

    /**
     * 요약/히스토리/메시지의 {@code YYYY-MM-DD ~ YYYY-MM-DD} 구간에 따른 달력 일수(양 끝 포함).
     * {@link RagSearchService} 의 {@code MAX_TRIP_DAYS} 와 맞춤.
     */
    private Integer expectedInclusiveCalendarDays(AiChatRequest request) {
        TripDateRange range = resolveTripDateRange(request);
        if (range == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(range.start(), range.end()) + 1;
        if (days < 1 || days > 21) {
            return null;
        }
        return (int) days;
    }

    private String buildTripCalendarDayConstraintSystemContent(AiChatRequest request) {
        Integer n = expectedInclusiveCalendarDays(request);
        if (n == null) {
            return "";
        }
        TripDateRange range = resolveTripDateRange(request);
        if (range == null) {
            return "";
        }
        return """
                TRIP_CALENDAR_DAYS: The user's trip spans exactly %d calendar day(s), inclusive from %s to %s.
                The JSON "days" array MUST contain exactly %d objects (one per calendar day, in order).
                Each object must have a "date" (YYYY-MM-DD matching that calendar day), a "label", and a non-empty "slots" array.
                Do not merge two calendar days into one; do not omit a middle day even if slots are light.
                """.formatted(n, range.start(), range.end(), n);
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

    /**
     * Groq가 date를 비우거나 잘못 넣은 경우에도 UI·지도가 ISO 일자를 쓰도록,
     * 요청 본문/히스토리에서 파싱한 YYYY-MM-DD ~ YYYY-MM-DD 구간에 맞춰 각 day의 date를 덮어쓴다.
     */
    @SuppressWarnings("unchecked")
    private void normalizeStructuredDayDates(List<?> dayList, AiChatRequest request) {
        TripDateRange range = resolveTripDateRange(request);
        if (range == null || dayList == null || dayList.isEmpty()) {
            return;
        }
        long span = ChronoUnit.DAYS.between(range.start(), range.end()) + 1;
        if (span < 1 || span > 21) {
            return;
        }
        for (int i = 0; i < dayList.size(); i++) {
            Object dayRaw = dayList.get(i);
            if (!(dayRaw instanceof Map<?, ?> dayMap)) {
                continue;
            }
            Map<String, Object> day = (Map<String, Object>) dayMap;
            LocalDate dayDate = i < span
                    ? range.start().plusDays(i)
                    : range.end();
            day.put("date", dayDate.toString());
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

    private String createRagSearchQuery(AiChatRequest request, List<TourCourse> savedCoursesForKeywords) {
        List<String> parts = new ArrayList<>();

        if (request.getHistory() != null) {
            List<AiChatRequest.ChatHistoryMessage> valid = request.getHistory().stream()
                    .filter(this::isValidHistoryMessage)
                    .toList();
            int ragHistoryLimit = Math.max(2, Math.min(12, promptMaxHistoryMessages));
            int hChars = Math.max(200, Math.min(800, promptMaxHistoryChars));
            int skip = Math.max(0, valid.size() - ragHistoryLimit);
            valid.stream()
                    .skip(skip)
                    .map(AiChatRequest.ChatHistoryMessage::getContent)
                    .map(content -> truncate(content, hChars))
                    .forEach(parts::add);
        }

        if (savedCoursesForKeywords != null) {
            int linesAdded = 0;
            for (TourCourse c : savedCoursesForKeywords) {
                if (c == null || linesAdded >= 4) {
                    break;
                }
                String title = truncate(nullToEmpty(c.getTitle()).trim(), 100);
                if (!title.isBlank()) {
                    parts.add(title);
                    linesAdded++;
                }
                if (linesAdded >= 4) {
                    break;
                }
                String tags = sanitizePromptLine(nullToEmpty(c.getHashtags()));
                if (!tags.isBlank()) {
                    parts.add(truncate(tags, 100));
                    linesAdded++;
                }
            }
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

            normalizeStructuredDayDates(dayList, request);

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
                    String sourceId = str(slot.get("sourceId"));
                    JsonNode matchedCandidate = findCandidateByPlaceName(candidates, placeName);
                    if (matchedCandidate == null) {
                        matchedCandidate = findCandidateBySourceId(candidates, sourceId);
                    }
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
    private boolean needsRetry(AiChatResponse response, AiChatRequest request) {
        if (response == null || response.getStructured() == null) return true;

        Object daysObj = response.getStructured().get("days");
        if (!(daysObj instanceof List<?> dayList) || dayList.isEmpty()) return true;

        Integer expectedDays = request != null ? expectedInclusiveCalendarDays(request) : null;
        if (expectedDays != null && dayList.size() < expectedDays) {
            log.info("[AI] needsRetry: day count {} < expected calendar days {}", dayList.size(), expectedDays);
            return true;
        }

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
                - Each day's "date" field MUST be strict YYYY-MM-DD only (ASCII digits and hyphens). Never put natural-language dates in "date" (e.g. not "May 12, 2026", not Korean "2026년 5월 12일", not "2026年5月12日"); the app formats that value for the user's UI locale.
                - Slots per day are FLEXIBLE (not always 6): use the user's flight arrival time and departure time (from the trip summary / chat, e.g. lines with 도착/arrive and 출발/depart) to decide which parts of the day are realistic.
                  - First day: if arrival is late morning or afternoon, omit 아침 and possibly 오전 코스; if arrival is evening, keep only 저녁 and/or 밤 코스 as appropriate.
                  - Last day: if departure is morning or before lunch, omit 밤 코스 and 저녁 (and 오후 코스 if needed); if departure is early afternoon, trim evening slots accordingly.
                  - Middle days: usually fill as fully as reasonable (often up to the full sequence below), still respecting travel time between slots.
                - Calendar coverage: If the trip summary or chat includes explicit trip dates as YYYY-MM-DD ~ YYYY-MM-DD (or TRIP_CALENDAR_DAYS appears), the "days" array MUST contain exactly one object per inclusive calendar day in that range (count both endpoints). Never merge two calendar days into one and never omit a middle day—if time is tight, use fewer slot types per day instead of dropping a day.
                - When you include multiple slots in one day, list them ONLY in this canonical order (subset allowed, never reorder): 아침 → 오전 코스 → 점심 → 오후 코스 → 저녁 → 밤 코스.
                - Prefer to pack the day densely when times allow: meal slots (아침, 점심, 저녁) with restaurant-like CANDIDATES; sightseeing slots (오전 코스, 오후 코스, 밤 코스) with attraction/culture-like CANDIDATES.
                - slot.type/label: one of 아침|오전 코스|점심|오후 코스|저녁|밤 코스 (keep these Korean tokens exactly for slot.type and slot.label so downstream parsers match)
                - slot.label == slot.type
                - slot.placeName: use CANDIDATES[].n first, never empty
                - Do not repeat the same placeName across all days/slots (case-insensitive). One place can appear only once.
                - slot.address: use CANDIDATES[].a, empty if unknown
                - slot.thumbnail: use CANDIDATES[].img when the compact list includes it; otherwise "" (the app may fill thumbnails after matching by placeName).
                - slot.reason: always use empty string "" (no narrative; place detail is shown elsewhere in the app).
                - Prefer restaurant-like candidates for 아침, 점심, and 저녁 slots, but use non-restaurant fallback when restaurant candidates are insufficient.
                - Prefer attraction/culture-like candidates for 오전 코스, 오후 코스, and 밤 코스 slots, but keep fallback flexible.
                - Prefer places likely open at the slot time (morning/lunch/afternoon/dinner/night).
                - Keep consecutive moves practical; avoid transitions that are likely over 45 minutes when alternatives exist.
                - Across days: the LAST slot of day N and the FIRST slot of day N+1 should also be within ~45 minutes travel when both have locations, unless the user clearly wants a long-distance jump (e.g. new city day).
                - Never put locker/luggage-storage POIs in slots. Lockers are not loaded into CANDIDATES for this flow; the app uses a separate nearest-locker API (first-day + last-day anchors only).
                - CANDIDATES key: n=name, a=address, c=category, h=operatingHours, st=sourceType, sid=sourceId, lat=latitude, lng=longitude, ls=localScore (0~1). Image URLs may be omitted in the compact CANDIDATES list to save tokens—leave slot.thumbnail empty when unsure.

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
                - If TRIP_CALENDAR_DAYS or explicit YYYY-MM-DD ~ YYYY-MM-DD dates exist in context, "days" length must equal inclusive calendar days; include every calendar day with non-empty slots (never skip a middle day).
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

    private JsonNode findCandidateBySourceId(ArrayNode candidates, String sourceId) {
        if (candidates == null || candidates.isEmpty() || sourceId == null || sourceId.isBlank()) {
            return null;
        }
        String want = sourceId.trim();
        for (JsonNode candidate : candidates) {
            String sid = candidate.path("sid").asText("").trim();
            if (!sid.isBlank() && sid.equals(want)) {
                return candidate;
            }
        }
        return null;
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
