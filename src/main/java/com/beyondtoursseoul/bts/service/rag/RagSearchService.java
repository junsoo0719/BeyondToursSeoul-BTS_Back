package com.beyondtoursseoul.bts.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RagSearchService {

    private static final int MAX_KEYWORD_COUNT = 16;
    private static final int MIN_RESULT_COUNT = 12;
    private static final int MAX_RESULT_COUNT = 18;
    private static final int MAX_CANDIDATE_COUNT = 80;
    private static final Pattern NIGHTS_DAYS_PATTERN = Pattern.compile("(\\d+)\\s*박\\s*(\\d+)\\s*일");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2})\\s*(?:~|〜|–|—|-)\\s*(\\d{4}-\\d{2}-\\d{2})"
    );
    private static final Set<String> STOPWORDS = Set.of(
            "서울", "여행", "일정", "계획", "코스", "생성", "추천", "장소", "좋은", "근처",
            "요청", "기간", "비행", "도착", "출발", "동행", "이동", "스타일", "테마", "추가",
            "박", "일", "2박", "3일", "필요", "없음", "중심", "렌트카", "유심",
            "알려줘", "해줘", "짜줘", "만들어줘", "부탁", "지금", "현재", "오늘", "내일",
            "친구", "혼자", "가족", "연인", "사람", "정도", "위주", "관련",
            "please", "recommend", "recommendation", "make", "create", "plan", "course",
            "trip", "travel", "place", "places", "near", "nearby", "good", "best"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RagSearchService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RagDocumentContext> search(String message, String language) {
        List<String> keywords = extractKeywords(message);
        if (keywords.isEmpty()) {
            log.info("[RAG] 검색 키워드 없음. message={}", summarize(message, 80));
            return List.of();
        }

        int tripDays = estimateTripDays(message);
        int resultLimit = resultLimitFor(tripDays);
        int candidateLimit = Math.min(MAX_CANDIDATE_COUNT, resultLimit * 5);
        List<String> langCodes = langCodes(language);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("langCodes", langCodes)
                .addValue("limit", candidateLimit);

        StringBuilder matchCondition = new StringBuilder();
        StringBuilder scoreExpression = new StringBuilder();

        for (int i = 0; i < keywords.size(); i++) {
            String paramName = "keyword" + i;
            params.addValue(paramName, "%" + keywords.get(i) + "%");

            if (i > 0) {
                matchCondition.append(" or ");
                scoreExpression.append(" + ");
            }

            matchCondition.append("""
                    (
                      content ilike :%1$s
                      or metadata::text ilike :%1$s
                      or title ilike :%1$s
                    )
                    """.formatted(paramName));

            scoreExpression.append("""
                    (
                      case when content ilike :%1$s then 30 else 0 end
                      + case when metadata::text ilike :%1$s then 10 else 0 end
                      + case when title ilike :%1$s then 5 else 0 end
                    )
                    """.formatted(paramName));
        }

        String sql = """
                select
                  id,
                  source_type,
                  source_id,
                  title,
                  content,
                  lang_code,
                  dong_code,
                  latitude,
                  longitude,
                  metadata::text as metadata,
                  %s as match_score
                from public.rag_documents
                where lang_code in (:langCodes)
                  and (%s)
                order by match_score desc, updated_at desc
                limit :limit
                """.formatted(scoreExpression, matchCondition);

        List<RagDocumentContext> candidates = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String sourceType = rs.getString("source_type");
            String title = rs.getString("title");
            String content = rs.getString("content");
            String metadata = rs.getString("metadata");

            return new RagDocumentContext(
                    rs.getLong("id"),
                    sourceType,
                    rs.getString("source_id"),
                    title,
                    content,
                    rs.getString("lang_code"),
                    rs.getString("dong_code"),
                    rs.getObject("latitude", Double.class),
                    rs.getObject("longitude", Double.class),
                    metadata,
                    rs.getInt("match_score"),
                    classifyCategory(sourceType, title, content, metadata)
            );
        });

        List<RagDocumentContext> results = diversifyResults(candidates, resultLimit, tripDays);
        logSearchResults(message, keywords, langCodes, tripDays, resultLimit, results);
        return results;
    }

    private List<RagDocumentContext> diversifyResults(
            List<RagDocumentContext> candidates,
            int resultLimit,
            int tripDays
    ) {
        List<RagDocumentContext> selected = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();

        addCategoryResults(candidates, selected, selectedIds, "restaurant", restaurantLimitFor(tripDays), resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "attraction", attractionLimitFor(tripDays), resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "night", Math.max(2, tripDays), resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "event", Math.max(2, tripDays), resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "shopping_kpop", Math.max(2, tripDays), resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "locker", tripDays >= 3 ? 2 : 1, resultLimit);

        for (RagDocumentContext candidate : candidates) {
            if (selected.size() >= resultLimit) {
                break;
            }

            if (selectedIds.add(candidate.id())) {
                selected.add(candidate);
            }
        }

        return selected;
    }

    private void addCategoryResults(
            List<RagDocumentContext> candidates,
            List<RagDocumentContext> selected,
            Set<Long> selectedIds,
            String category,
            int limit,
            int resultLimit
    ) {
        int added = 0;
        for (RagDocumentContext candidate : candidates) {
            if (selected.size() >= resultLimit || added >= limit) {
                return;
            }

            if (category.equals(candidate.category()) && selectedIds.add(candidate.id())) {
                selected.add(candidate);
                added++;
            }
        }
    }

    private void logSearchResults(
            String message,
            List<String> keywords,
            List<String> langCodes,
            int tripDays,
            int resultLimit,
            List<RagDocumentContext> results
    ) {
        log.info("[RAG] message='{}', langCodes={}, tripDays={}, resultLimit={}, keywords={}, resultCount={}",
                summarize(message, 80), langCodes, tripDays, resultLimit, keywords, results.size());

        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            RagDocumentContext result = results.get(i);
            log.info("[RAG] #{} category={}, type={}, title='{}', lang={}, dong={}, score={}, content='{}'",
                    i + 1,
                    result.category(),
                    result.sourceType(),
                    summarize(result.title(), 60),
                    result.langCode(),
                    result.dongCode(),
                    result.matchScore(),
                    summarize(result.content(), 120));
        }
    }

    private int estimateTripDays(String message) {
        if (message == null || message.isBlank()) {
            return 1;
        }

        Matcher nightsDaysMatcher = NIGHTS_DAYS_PATTERN.matcher(message);
        if (nightsDaysMatcher.find()) {
            return clampTripDays(Integer.parseInt(nightsDaysMatcher.group(2)));
        }

        Matcher dateRangeMatcher = DATE_RANGE_PATTERN.matcher(message);
        if (dateRangeMatcher.find()) {
            try {
                LocalDate startDate = LocalDate.parse(dateRangeMatcher.group(1));
                LocalDate endDate = LocalDate.parse(dateRangeMatcher.group(2));
                long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                return clampTripDays((int) days);
            } catch (RuntimeException e) {
                return 1;
            }
        }

        return 1;
    }

    private int clampTripDays(int days) {
        return Math.max(1, Math.min(days, 3));
    }

    private int resultLimitFor(int tripDays) {
        return Math.min(MAX_RESULT_COUNT, Math.max(MIN_RESULT_COUNT, tripDays * 6));
    }

    private int restaurantLimitFor(int tripDays) {
        return Math.min(18, Math.max(3, tripDays * 3));
    }

    private int attractionLimitFor(int tripDays) {
        return Math.min(18, Math.max(3, tripDays * 3));
    }

    private String summarize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String classifyCategory(String sourceType, String title, String content, String metadata) {
        String source = normalize(sourceType);
        String text = normalize(title + " " + content + " " + metadata);

        if (source.contains("locker") || containsAny(text, "물품보관함", "보관함", "짐", "locker")) {
            return "locker";
        }

        if (containsAny(text, "음식점", "음식", "식당", "맛집", "한식", "중식", "일식", "카페", "시장")) {
            return "restaurant";
        }

        if (containsAny(text, "야경", "전망", "루프탑", "한강", "타워", "밤", "바", "bar")) {
            return "night";
        }

        if (source.contains("event") || containsAny(text, "행사", "축제", "공연", "전시", "문화행사")) {
            return "event";
        }

        if (containsAny(text, "k-pop", "kpop", "케이팝", "아이돌", "엔터테인먼트", "쇼핑", "면세점")) {
            return "shopping_kpop";
        }

        if (containsAny(text, "관광지", "문화시설", "레포츠", "공원", "궁", "박물관", "미술관")) {
            return "attraction";
        }

        return "other";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private List<String> extractKeywords(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        String normalized = message.toLowerCase(Locale.ROOT);

        addThemeKeywords(normalized, keywords);

        for (String token : normalized.split("[^\\p{IsHangul}\\p{Alnum}]+")) {
            if (isSearchableToken(token)) {
                keywords.add(token);
            }
        }

        return new ArrayList<>(keywords).stream()
                .limit(MAX_KEYWORD_COUNT)
                .toList();
    }

    private boolean isSearchableToken(String token) {
        return token.length() >= 2
                && !STOPWORDS.contains(token)
                && !token.matches("\\d+")
                && !token.matches("\\d+박")
                && !token.matches("\\d+일")
                && !token.matches("\\d{4}")
                && !token.matches("\\d{2}:\\d{2}");
    }

    private void addThemeKeywords(String message, Set<String> keywords) {
        if (containsAny(message, "야경", "전망", "루프탑", "밤", "night", "view")) {
            keywords.addAll(List.of("야경", "전망", "한강", "타워", "루프탑", "밤"));
        }

        if (containsAny(message, "맛집", "음식", "식당", "한식", "로컬", "local", "food")) {
            keywords.addAll(List.of("음식점", "식당", "시장", "한식", "로컬", "맛집"));
        }

        if (containsAny(message, "k-pop", "kpop", "케이팝", "아이돌", "idol")) {
            keywords.addAll(List.of("케이팝", "아이돌", "공연", "콘서트", "엔터테인먼트"));
        }

        if (containsAny(message, "문화", "전시", "행사", "축제", "공연")) {
            keywords.addAll(List.of("문화", "전시", "행사", "축제", "공연"));
        }

        if (containsAny(message, "보관", "짐", "락커", "locker", "luggage")) {
            keywords.addAll(List.of("물품보관함", "보관함", "짐", "역명"));
        }
    }

    private boolean containsAny(String message, String... values) {
        for (String value : values) {
            if (message.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private List<String> langCodes(String language) {
        String normalized = language == null ? "ko" : language.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "en", "eng" -> List.of("en", "eng");
            case "ja", "jp", "jpn" -> List.of("ja", "jpn");
            case "zh", "chs", "cht", "cn" -> List.of("zh", "chs", "cht");
            default -> List.of("ko", "kor");
        };
    }

    public record RagDocumentContext(
            Long id,
            String sourceType,
            String sourceId,
            String title,
            String content,
            String langCode,
            String dongCode,
            Double latitude,
            Double longitude,
            String metadata,
            Integer matchScore,
            String category
    ) {
    }
}
