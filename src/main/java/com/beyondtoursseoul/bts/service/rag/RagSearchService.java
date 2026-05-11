package com.beyondtoursseoul.bts.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
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
    /** 식당·관광 후보: 일수당 각 5건 (프롬프트·토큰 부담 완화) */
    private static final int PER_DAY_RESTAURANT = 5;
    private static final int PER_DAY_ATTRACTION = 5;
    /** 입력 기간 파싱 상한 (비정상 메시지·과도한 SQL limit 방지) */
    private static final int MAX_TRIP_DAYS = 21;
    /** scored CTE에서 가져오는 최대 행 수 (다양화 전 풀) */
    private static final int MAX_CANDIDATE_COUNT = 400;
    /** diversify 결과 최대 건수 */
    private static final int MAX_RESULT_CAP = 400;
    private static final int DEFAULT_LOCAL_RATIO = 50;
    private static final Pattern NIGHTS_DAYS_PATTERN = Pattern.compile("(\\d+)\\s*박\\s*(\\d+)\\s*일");
    private static final Pattern GROUP_SIZE_PATTERN = Pattern.compile("(\\d+)\\s*명");
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
        return search(message, language, DEFAULT_LOCAL_RATIO);
    }

    public List<RagDocumentContext> search(String message, String language, int localRatio) {
        List<String> keywords = extractKeywords(message);
        if (keywords.isEmpty()) {
            log.info("[RAG] 검색 키워드 없음. message={}", summarize(message, 80));
            return List.of();
        }

        int tripDays = estimateTripDays(message);
        int resultLimit = resultLimitFor(tripDays);
        int candidateLimit = Math.min(MAX_CANDIDATE_COUNT, Math.max(80, (int) Math.ceil(resultLimit * 2.5)));
        String transportPreference = resolveTransportPreference(message);
        Set<String> companionPreferences = resolveCompanionPreferences(message);
        int groupSize = estimateGroupSize(message);
        List<String> themePreferences = resolveThemePreferences(message);
        List<String> langCodes = langCodes(language);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("langCodes", langCodes)
                .addValue("limit", candidateLimit)
                .addValue("preferPublicTransport", "public".equals(transportPreference))
                .addValue("preferCarTransport", "car".equals(transportPreference))
                .addValue("preferSolo", companionPreferences.contains("solo"))
                .addValue("preferCouple", companionPreferences.contains("couple"))
                .addValue("preferFriends", companionPreferences.contains("friends"))
                .addValue("preferFamily", companionPreferences.contains("family"))
                .addValue("groupSize", groupSize)
                .addValue("hasGroupSize", groupSize > 0)
                .addValue("hasTheme1", themePreferences.size() >= 1)
                .addValue("hasTheme2", themePreferences.size() >= 2)
                .addValue("hasTheme3", themePreferences.size() >= 3)
                .addValue("theme1", themePreferences.size() >= 1 ? "%" + themePreferences.get(0) + "%" : "")
                .addValue("theme2", themePreferences.size() >= 2 ? "%" + themePreferences.get(1) + "%" : "")
                .addValue("theme3", themePreferences.size() >= 3 ? "%" + themePreferences.get(2) + "%" : "");

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

        // localRatio 0~100 → target 0.0~1.0
        double targetLocal = Math.max(0.0, Math.min(100, localRatio)) / 100.0;
        params.addValue("targetLocal", targetLocal);

        /*
         * alignment_score = local_alignment + keyword_alignment + zone_alignment
         *   - local_alignment: 로컬 선호(localRatio)와 동 점수의 정렬
         *   - keyword_alignment: 질의 키워드 일치도
         *   - zone_alignment: 동일 권역(dong_code) 응집도 가점
         *
         * zone_alignment 규칙:
         *   1) 1차 후보군에서 가장 많이 등장한 dong_code를 dominant zone으로 계산
         *   2) 해당 zone 문서에 가점을 부여해 하루 동선이 한 권역에 묶이도록 유도
         *   3) dong_code가 없는 문서는 중립값(0.5) 적용
         */
        String sql = """
                with latest_dong_score as (
                    select dls.dong_code, max(dls.score)::double precision as local_score
                    from public.dong_local_score dls
                    where dls.date = (select max(date) from public.dong_local_score)
                    group by dls.dong_code
                ),
                scored as (
                    select
                      rd.id,
                      rd.source_type,
                      rd.source_id,
                      rd.title,
                      rd.content,
                      rd.lang_code,
                      rd.dong_code,
                      rd.latitude,
                      rd.longitude,
                      rd.metadata::text as metadata,
                      coalesce(lds.local_score, 0.5)::double precision as local_score,
                      (
                        case
                          when :preferPublicTransport then coalesce(pe.score_transport, 50)::double precision / 100.0
                          when :preferCarTransport then coalesce(pe.score_car, 50)::double precision / 100.0
                          else 0.5
                        end
                      ) as transport_score,
                      (
                        case
                          when (:preferSolo and coalesce(pe.recommended_companion_types::text, '') ilike '%%solo%%') then 1.0
                          when (:preferCouple and coalesce(pe.recommended_companion_types::text, '') ilike '%%couple%%') then 1.0
                          when (:preferFriends and coalesce(pe.recommended_companion_types::text, '') ilike '%%friends%%') then 1.0
                          when (:preferFamily and coalesce(pe.recommended_companion_types::text, '') ilike '%%family%%') then 1.0
                          else 0.5
                        end
                      ) as companion_score,
                      (
                        case
                          when :hasGroupSize
                               and pe.min_group_size is not null
                               and pe.max_group_size is not null
                               and :groupSize between pe.min_group_size and pe.max_group_size
                            then 1.0
                          when :hasGroupSize then 0.0
                          else 0.5
                        end
                      ) as group_score,
                      (
                        case
                          when :hasTheme1 or :hasTheme2 or :hasTheme3 then
                            case
                              when ((:hasTheme1 and coalesce(pe.recommended_category, '') ilike :theme1)
                                 or (:hasTheme2 and coalesce(pe.recommended_category, '') ilike :theme2)
                                 or (:hasTheme3 and coalesce(pe.recommended_category, '') ilike :theme3))
                                then 1.0
                              else 0.0
                            end
                          else 0.5
                        end
                      ) as theme_score,
                      least(greatest(coalesce(pe.score_fit, 0.5)::double precision, 0.0), 1.0) as fit_score,
                      (%s)::double precision as match_score
                    from public.rag_documents rd
                    left join latest_dong_score lds on lds.dong_code = rd.dong_code
                    left join public.place_enrichment pe
                      on pe.source_type = rd.source_type
                     and pe.source_id = rd.source_id
                    where rd.lang_code in (:langCodes)
                      and lower(coalesce(rd.source_type, '')) not like '%locker%'
                      and (%s)
                ),
                max_match as (
                    select greatest(max(match_score), 1) as val from scored
                ),
                dominant_zone as (
                    select s.dong_code
                    from scored s
                    where s.dong_code is not null
                    group by s.dong_code
                    order by count(*) desc, max(s.match_score) desc
                    limit 1
                )
                select
                  s.*,
                  (
                    0.50 * (1.0 - abs(s.local_score - :targetLocal))
                    + 0.20 * (s.match_score / mm.val)
                    + 0.20 * (
                        case
                            when s.dong_code is null then 0.5
                            when dz.dong_code is null then 0.5
                            when s.dong_code = dz.dong_code then 1.0
                            else 0.0
                        end
                    )
                    + 0.10 * (
                        0.30 * s.transport_score
                        + 0.25 * s.companion_score
                        + 0.20 * s.group_score
                        + 0.15 * s.theme_score
                        + 0.10 * s.fit_score
                    )
                  ) as alignment_score
                from scored s
                cross join max_match mm
                left join dominant_zone dz on true
                order by alignment_score desc, s.match_score desc
                limit :limit
                """.formatted(scoreExpression, matchCondition);

        List<RagDocumentContext> raw = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
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
                    classifyCategory(sourceType, title, content, metadata),
                    rs.getObject("local_score", Double.class),
                    rs.getObject("alignment_score", Double.class)
            );
        });

        List<RagDocumentContext> candidates = new ArrayList<>();
        for (RagDocumentContext row : raw) {
            if (!isLockerRagRow(row)) {
                candidates.add(row);
            }
        }

        List<RagDocumentContext> results = diversifyResults(candidates, resultLimit, tripDays);
        logSearchResults(message, keywords, langCodes, tripDays, resultLimit, localRatio, results);
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
        int ancillaryLimit = ancillaryCategoryLimit(tripDays);
        addCategoryResults(candidates, selected, selectedIds, "night", ancillaryLimit, resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "event", ancillaryLimit, resultLimit);
        addCategoryResults(candidates, selected, selectedIds, "shopping_kpop", ancillaryLimit, resultLimit);

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

    /** AI 코스용 RAG에서는 물품보관함 문서를 쓰지 않음(앱에서 nearest API). */
    private boolean isLockerRagRow(RagDocumentContext c) {
        if (c == null) {
            return true;
        }
        String cat = c.category();
        if (cat != null && "locker".equalsIgnoreCase(cat.trim())) {
            return true;
        }
        String st = c.sourceType();
        return st != null && st.toLowerCase(Locale.ROOT).contains("locker");
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
            int localRatio,
            List<RagDocumentContext> results
    ) {
        log.info("[RAG] message='{}', langCodes={}, tripDays={}, resultLimit={}, localRatio={}, keywords={}, resultCount={}",
                summarize(message, 80), langCodes, tripDays, resultLimit, localRatio, keywords, results.size());

        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            RagDocumentContext result = results.get(i);
            log.info("[RAG] #{} category={}, type={}, title='{}', dong={}, localScore={}, alignment={}, content='{}'",
                    i + 1,
                    result.category(),
                    result.sourceType(),
                    summarize(result.title(), 60),
                    result.dongCode(),
                    result.localScore() != null ? String.format("%.2f", result.localScore()) : "n/a",
                    result.alignmentScore() != null ? String.format("%.3f", result.alignmentScore()) : "n/a",
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
        return Math.max(1, Math.min(days, MAX_TRIP_DAYS));
    }

    /**
     * diversify 단계에서 선택할 최대 문서 수.
     * 식당·관광(일수×10) + 야경·행사·쇼핑(일수 기반, 상한). 락커는 RAG에 넣지 않음(앱에서 nearest API).
     */
    private int resultLimitFor(int tripDays) {
        int t = clampTripDays(tripDays);
        int restaurants = PER_DAY_RESTAURANT * t;
        int attractions = PER_DAY_ATTRACTION * t;
        int anc = ancillaryCategoryLimit(tripDays);
        int budget = restaurants + attractions + 3 * anc;
        return Math.min(MAX_RESULT_CAP, Math.max(12, budget));
    }

    /** 야경·행사·K-pop/쇼핑: 짧은 일정은 최소 2, 길어지면 일수까지 늘리되 과도한 비중은 10건으로 캡 */
    private int ancillaryCategoryLimit(int tripDays) {
        int t = clampTripDays(tripDays);
        return Math.max(2, Math.min(t, 10));
    }

    private int restaurantLimitFor(int tripDays) {
        return PER_DAY_RESTAURANT * clampTripDays(tripDays);
    }

    private int attractionLimitFor(int tripDays) {
        return PER_DAY_ATTRACTION * clampTripDays(tripDays);
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

    private String resolveTransportPreference(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "대중교통", "지하철", "버스", "도보", "걸어서", "subway", "metro", "walk")) {
            return "public";
        }
        if (containsAny(normalized, "차량", "자차", "렌트", "드라이브", "주차", "car", "drive", "parking")) {
            return "car";
        }
        return "neutral";
    }

    private Set<String> resolveCompanionPreferences(String message) {
        String normalized = normalize(message);
        Set<String> out = new HashSet<>();
        if (containsAny(normalized, "혼자", "1인", "solo", "single")) out.add("solo");
        if (containsAny(normalized, "커플", "연인", "데이트", "couple", "romantic")) out.add("couple");
        if (containsAny(normalized, "친구", "우정", "friends", "group")) out.add("friends");
        if (containsAny(normalized, "가족", "아이", "키즈", "부모님", "family", "kids")) out.add("family");
        return out;
    }

    private int estimateGroupSize(String message) {
        if (message == null || message.isBlank()) return 0;
        Matcher m = GROUP_SIZE_PATTERN.matcher(message);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (RuntimeException ignored) {
            }
        }
        String normalized = normalize(message);
        if (containsAny(normalized, "혼자", "1인", "solo")) return 1;
        if (containsAny(normalized, "커플", "연인", "둘이", "2명", "couple")) return 2;
        if (containsAny(normalized, "가족", "family")) return 4;
        if (containsAny(normalized, "친구들", "친구", "friends")) return 3;
        return 0;
    }

    private List<String> resolveThemePreferences(String message) {
        String normalized = normalize(message);
        LinkedHashSet<String> themes = new LinkedHashSet<>();
        if (containsAny(normalized, "k-pop", "kpop", "케이팝", "아이돌")) themes.add("K-팝");
        if (containsAny(normalized, "맛집", "음식", "식당", "카페", "food", "restaurant")) themes.add("맛집");
        if (containsAny(normalized, "쇼핑", "shopping", "mall", "시장")) themes.add("쇼핑");
        if (containsAny(normalized, "역사", "문화", "박물관", "궁", "heritage")) themes.add("역사");
        if (containsAny(normalized, "전시", "아트", "미술", "gallery", "exhibition")) themes.add("예술");
        if (containsAny(normalized, "자연", "공원", "한강", "숲", "park")) themes.add("자연");
        if (containsAny(normalized, "야경", "루프탑", "바", "night", "bar")) themes.add("야경");
        if (containsAny(normalized, "힐링", "휴식", "relax")) themes.add("힐링");
        return new ArrayList<>(themes).stream().limit(3).toList();
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
            String category,
            Double localScore,
            Double alignmentScore
    ) {
    }
}
