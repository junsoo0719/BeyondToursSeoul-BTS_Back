package com.beyondtoursseoul.bts.service.course;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionLocalScore;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.course.*;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.course.TourCourseDetailResponse;
import com.beyondtoursseoul.bts.dto.course.TourCourseItemResponse;
import com.beyondtoursseoul.bts.dto.course.TourCourseSummaryResponse;
import com.beyondtoursseoul.bts.repository.AttractionLocalScoreRepository;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.AttractionTranslationRepository;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseItemRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseItemTranslationRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseTranslationRepository;
import com.beyondtoursseoul.bts.repository.course.UserSavedCourseRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import com.beyondtoursseoul.bts.service.score.TimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCourseService {

    private static final String COURSE_LOCAL_SCORE_TIME_SLOT = TimeSlot.AFTERNOON.getCode();

    /** 연속 관광지 스팟이 이보다 가깝게 붙어 있으면(또는 동일 ID 중복이면) 응답 시 근처 다른 관광지로 치환한다. */
    private static final double MIN_SPOT_SEPARATION_METERS = 150.0;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /** 종료된 코스 행사를 다른 행사로 바꿀 때, 원래 행사 위치에서 이 거리(m)를 넘기면 관광지로 대체한다. */
    private static final double MAX_EVENT_REPLACEMENT_DISTANCE_M = 300.0;
    /** 코스 상세 스텝 코멘트에 넣는 관광지 개요(overview) 최대 길이. */
    private static final int COURSE_SPOT_COMMENT_MAX_CHARS = 800;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final TourCourseRepository tourCourseRepository;
    private final TourCourseItemRepository tourCourseItemRepository;
    private final TourCourseTranslationRepository tourCourseTranslationRepository;
    private final UserSavedCourseRepository userSavedCourseRepository;
    private final ProfileRepository profileRepository;
    private final TourApiEventTranslationRepository eventTranslationRepository;
    private final TourApiEventRepository tourApiEventRepository;
    private final AttractionLocalScoreRepository attractionLocalScoreRepository;
    private final AttractionRepository attractionRepository;
    private final AttractionTranslationRepository attractionTranslationRepository;
    private final TourCourseItemTranslationRepository tourCourseItemTranslationRepository;

    private record CourseLocalRollup(BigDecimal avgLocalScore, Integer avgLocalScorePercent, Integer localBand) {
    }

    /**
     * 추천 여행 코스 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TourCourseSummaryResponse> getCourseList(TourLanguage lang, UUID userId) {
        Profile user = userId != null ? profileRepository.findById(userId).orElse(null) : null;

        List<TourCourse> courses = tourCourseRepository.findAll();
        Set<Long> courseIds = courses.stream().map(TourCourse::getId).collect(Collectors.toSet());
        Map<Long, CourseLocalRollup> rollups = loadCourseLocalRollups(courseIds);
        Map<Long, String> firstSpotImageByCourseId = loadFirstSpotThumbnailUrlByCourseId(courseIds);
        Map<Long, Map<TourLanguage, TourCourseTranslation>> translationsByCourse =
                loadTranslationsGroupedByCourse(courseIds);
        Set<Long> savedCourseIds = user != null
                ? userSavedCourseRepository.findSavedCourseIdsByUserId(user.getId())
                : Set.of();

        return courses.stream()
                .map(course -> {
                    TourCourseTranslation translation = resolveCourseTranslation(course, lang, translationsByCourse);
                    boolean isSaved = user != null && savedCourseIds.contains(course.getId());
                    CourseLocalRollup r = rollups.get(course.getId());

                    return TourCourseSummaryResponse.builder()
                            .id(course.getId())
                            .title(translation.getTitle())
                            .hashtags(translation.getHashtags())
                            .featuredImage(resolveListFeaturedImage(course, firstSpotImageByCourseId))
                            .isSaved(isSaved)
                            .avgLocalScore(r.avgLocalScore())
                            .avgLocalScorePercent(r.avgLocalScorePercent())
                            .localBand(r.localBand())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 추천 여행 코스 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public TourCourseDetailResponse getCourseDetail(Long courseId, TourLanguage lang, UUID userId) {
        TourCourse course = tourCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다. ID: " + courseId));

        Profile user = userId != null ? profileRepository.findById(userId).orElse(null) : null;
        TourCourseTranslation translation = getTranslation(course, lang);
        boolean isSaved = user != null && userSavedCourseRepository.existsByUserAndCourse(user, course);

        List<TourCourseItem> items = tourCourseItemRepository.findByCourseWithSpotsFetchedOrderBySequence(course);
        List<TourCourseItemResponse> itemResponses = buildDetailItemsResolvingSpots(items, lang);

        CourseLocalRollup r = loadCourseLocalRollups(Set.of(course.getId())).get(course.getId());

        String featuredImage = course.getFeaturedImage() != null ? course.getFeaturedImage() : "";
        if (!itemResponses.isEmpty()) {
            String firstThumb = itemResponses.get(0).getThumbnail();
            if (firstThumb != null && !firstThumb.isBlank()) {
                featuredImage = firstThumb.trim();
            }
        }

        return TourCourseDetailResponse.builder()
                .id(course.getId())
                .title(translation.getTitle())
                .hashtags(translation.getHashtags())
                .featuredImage(featuredImage)
                .isSaved(isSaved)
                .items(itemResponses)
                .avgLocalScore(r.avgLocalScore())
                .avgLocalScorePercent(r.avgLocalScorePercent())
                .localBand(r.localBand())
                .build();
    }

    /**
     * 코스를 '저장됨' 목록에 추가하거나 삭제합니다 (Toggle).
     */
    @Transactional
    public boolean toggleSaveCourse(Long courseId, UUID userId) {
        TourCourse course = tourCourseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Optional<UserSavedCourse> saved = userSavedCourseRepository.findByUserAndCourse(user, course);

        if (saved.isPresent()) {
            userSavedCourseRepository.delete(saved.get());
            return false; // 저장 취소
        } else {
            UserSavedCourse userSavedCourse = UserSavedCourse.builder()
                    .user(user)
                    .course(course)
                    .build();
            userSavedCourseRepository.save(userSavedCourse);
            return true; // 저장 완료
        }
    }

    /**
     * 사용자가 저장한 코스 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TourCourseSummaryResponse> getSavedCourses(TourLanguage lang, UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<UserSavedCourse> savedRows = userSavedCourseRepository.findByUserOrderBySavedAtDesc(user);
        Set<Long> courseIds = savedRows.stream()
                .map(s -> s.getCourse().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, CourseLocalRollup> rollups = loadCourseLocalRollups(courseIds);
        Map<Long, String> firstSpotImageByCourseId = loadFirstSpotThumbnailUrlByCourseId(courseIds);

        return savedRows.stream()
                .map(saved -> {
                    TourCourse course = saved.getCourse();
                    TourCourseTranslation translation = getTranslation(course, lang);
                    CourseLocalRollup r = rollups.get(course.getId());
                    return TourCourseSummaryResponse.builder()
                            .id(course.getId())
                            .title(translation.getTitle())
                            .hashtags(translation.getHashtags())
                            .featuredImage(resolveListFeaturedImage(course, firstSpotImageByCourseId))
                            .isSaved(true)
                            .avgLocalScore(r.avgLocalScore())
                            .avgLocalScorePercent(r.avgLocalScorePercent())
                            .localBand(r.localBand())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 코스에 포함된 관광지(행사 제외)의 찐로컬 지수를 최신일·오후 슬롯 기준으로 평균합니다.
     * 디스커버 관광지 목록 API 기본값(timeSlot=afternoon)과 맞춥니다.
     */
    private Map<Long, CourseLocalRollup> loadCourseLocalRollups(Set<Long> courseIds) {
        Map<Long, CourseLocalRollup> out = new HashMap<>();
        if (courseIds == null || courseIds.isEmpty()) {
            return out;
        }
        for (Long courseId : courseIds) {
            out.put(courseId, new CourseLocalRollup(null, null, null));
        }

        List<TourCourseItem> items = tourCourseItemRepository.findByCourseIdInWithAttractionFetched(
                courseIds, CourseItemType.ATTRACTION);

        Map<Long, LinkedHashSet<Long>> courseToAttractionIds = new HashMap<>();
        for (Long courseId : courseIds) {
            courseToAttractionIds.put(courseId, new LinkedHashSet<>());
        }
        for (TourCourseItem item : items) {
            if (item.getCourse() == null || item.getAttraction() == null) {
                continue;
            }
            Long cid = item.getCourse().getId();
            if (!courseToAttractionIds.containsKey(cid)) {
                continue;
            }
            courseToAttractionIds.get(cid).add(item.getAttraction().getId());
        }

        Set<Long> allAttractionIds = courseToAttractionIds.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        if (allAttractionIds.isEmpty()) {
            return out;
        }

        Optional<LocalDate> latest = attractionLocalScoreRepository.findLatestDate();
        if (latest.isEmpty()) {
            return out;
        }
        LocalDate date = latest.get();

        List<AttractionLocalScore> scoreRows = attractionLocalScoreRepository.findByDateAndTimeSlotAndAttractionIdIn(
                date, COURSE_LOCAL_SCORE_TIME_SLOT, allAttractionIds);

        Map<Long, BigDecimal> scoreByAttractionId = scoreRows.stream()
                .collect(Collectors.toMap(s -> s.getId().getAttractionId(), AttractionLocalScore::getScore, (a, b) -> a));

        for (Long courseId : courseIds) {
            Set<Long> aids = courseToAttractionIds.get(courseId);
            List<BigDecimal> values = aids.stream()
                    .map(scoreByAttractionId::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (values.isEmpty()) {
                continue;
            }
            BigDecimal avg = average(values);
            if (avg == null) {
                continue;
            }
            int pct = avg.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
            pct = Math.min(100, Math.max(0, pct));
            out.put(courseId, new CourseLocalRollup(avg, pct, localBandFromAvg(avg)));
        }
        return out;
    }

    /**
     * 코스 목록/저장 카드용: 각 코스의 순서상 첫 스팟 썸네일 URL (상세 API 첫 장소와 동일 기준).
     */
    private Map<Long, String> loadFirstSpotThumbnailUrlByCourseId(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        List<TourCourseItem> rows = tourCourseItemRepository.findByCourseIdInWithSpotsFetchedOrdered(courseIds);
        Map<Long, String> out = new LinkedHashMap<>();
        for (TourCourseItem item : rows) {
            if (item.getCourse() == null) {
                continue;
            }
            Long cid = item.getCourse().getId();
            if (out.containsKey(cid)) {
                continue;
            }
            String url = thumbnailUrlFromCourseItem(item);
            if (url != null && !url.isBlank()) {
                out.put(cid, url.trim());
            }
        }
        return out;
    }

    private String thumbnailUrlFromCourseItem(TourCourseItem item) {
        if (item == null) {
            return "";
        }
        if (item.getItemType() == CourseItemType.ATTRACTION && item.getAttraction() != null) {
            String t = item.getAttraction().getThumbnail();
            return t != null ? t.trim() : "";
        }
        if (item.getItemType() == CourseItemType.EVENT && item.getEvent() != null) {
            String t = item.getEvent().getFirstImage();
            return t != null ? t.trim() : "";
        }
        return "";
    }

    private String resolveListFeaturedImage(TourCourse course, Map<Long, String> firstSpotUrlByCourseId) {
        if (course == null) {
            return "";
        }
        if (firstSpotUrlByCourseId != null) {
            String fromItem = firstSpotUrlByCourseId.get(course.getId());
            if (fromItem != null && !fromItem.isBlank()) {
                return fromItem.trim();
            }
        }
        return course.getFeaturedImage() != null ? course.getFeaturedImage() : "";
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 디스커버 화면의 찐로컬 밀도 5단계와 동일한 경계(0, ~0.30, ~0.50, ~0.70, 그 위).
     */
    private static Integer localBandFromAvg(BigDecimal avg) {
        if (avg == null) {
            return null;
        }
        if (avg.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (avg.compareTo(new BigDecimal("0.30")) <= 0) {
            return 1;
        }
        if (avg.compareTo(new BigDecimal("0.50")) <= 0) {
            return 2;
        }
        if (avg.compareTo(new BigDecimal("0.70")) <= 0) {
            return 3;
        }
        return 4;
    }

    private TourCourseTranslation getTranslation(TourCourse course, TourLanguage lang) {
        return tourCourseTranslationRepository.findByCourseAndLanguage(course, lang)
                .orElseGet(() -> tourCourseTranslationRepository.findByCourseAndLanguage(course, TourLanguage.KOR)
                        .orElse(TourCourseTranslation.builder()
                                .title(course.getTitle())
                                .hashtags(course.getHashtags())
                                .build()));
    }

    /**
     * 목록 N+1 방지: 코스별 번역을 한 번에 로드한 뒤, 요청 언어 → 국문 → 엔티티 기본값 순으로 고른다.
     */
    private Map<Long, Map<TourLanguage, TourCourseTranslation>> loadTranslationsGroupedByCourse(Set<Long> courseIds) {
        Map<Long, Map<TourLanguage, TourCourseTranslation>> out = new HashMap<>();
        if (courseIds == null || courseIds.isEmpty()) {
            return out;
        }
        for (TourCourseTranslation t : tourCourseTranslationRepository.findByCourse_IdIn(courseIds)) {
            if (t.getCourse() == null) {
                continue;
            }
            Long cid = t.getCourse().getId();
            out.computeIfAbsent(cid, k -> new EnumMap<>(TourLanguage.class)).put(t.getLanguage(), t);
        }
        return out;
    }

    private TourCourseTranslation resolveCourseTranslation(
            TourCourse course,
            TourLanguage lang,
            Map<Long, Map<TourLanguage, TourCourseTranslation>> translationsByCourse) {
        if (course == null) {
            return TourCourseTranslation.builder().title("").hashtags("").build();
        }
        Map<TourLanguage, TourCourseTranslation> byLang = translationsByCourse.get(course.getId());
        if (byLang != null) {
            TourCourseTranslation preferred = byLang.get(lang);
            if (preferred != null) {
                return preferred;
            }
            TourCourseTranslation kor = byLang.get(TourLanguage.KOR);
            if (kor != null) {
                return kor;
            }
        }
        return TourCourseTranslation.builder()
                .title(course.getTitle())
                .hashtags(course.getHashtags())
                .build();
    }

    /**
     * {@code attraction_translation.lang} 컬럼 값(en/ja/zh)과 맞춘다. ({@link AttractionQueryService}와 동일 규칙)
     */
    private static String attractionDbLangCode(TourLanguage lang) {
        if (lang == null || lang == TourLanguage.KOR) {
            return "ko";
        }
        return switch (lang) {
            case ENG -> "en";
            case JPN -> "ja";
            case CHS, CHT -> "zh";
            default -> "ko";
        };
    }

    /** 연속 스팟 중복·과밀 시 근처 다른 관광지로 치환할 때 {@code ai_comment}에 붙는 안내 문구. */
    private static String overlapReplacementCommentSuffix(TourLanguage lang) {
        TourLanguage l = lang != null ? lang : TourLanguage.KOR;
        return switch (l) {
            case ENG -> "This spot overlapped or was too close to the previous one, so we suggested another nearby attraction in the same area.";
            case JPN -> "前のスポットと重複するか距離が近すぎるため、同じ付近から別の観光地をおすすめしました。";
            case CHS -> "与上一景点重复或距离过近，因此根据同一位置推荐了附近的其他景点。";
            case CHT -> "與上一景點重複或距離過近，因此根據同一位置推薦了附近的其他景點。";
            case KOR -> "앞선 스팟과 겹치거나 너무 가까워, 같은 위치 기준으로 가까운 다른 관광지를 추천했습니다.";
        };
    }

    /** 종료된 행사를 가까운 다른 행사로 바꿀 때 {@code ai_comment}에 붙는 안내 문구. */
    private static String eventExpiredNearEventSuffix(TourLanguage lang) {
        TourLanguage l = lang != null ? lang : TourLanguage.KOR;
        return switch (l) {
            case ENG -> "The festival or event shown here has ended; we replaced it with another event happening nearby.";
            case JPN -> "掲載の祭り・イベントは終了しているため、同じ付近で開催中の別のイベントに差し替えました。";
            case CHS -> "原节日或活动已结束，已替换为附近正在举办的另一场活动。";
            case CHT -> "原節日或活動已結束，已替換為附近正在舉辦的另一場活動。";
            case KOR -> "종료된 축제·행사 대신, 같은 근처에서 열리는 다른 행사로 안내했습니다.";
        };
    }

    /** 종료된 행사를 관광지로 바꿀 때, 코스 스텝 설명이 비었을 때 쓰는 짧은 안내. */
    private static String eventExpiredReplacedByAttractionFallback(TourLanguage lang) {
        TourLanguage l = lang != null ? lang : TourLanguage.KOR;
        return switch (l) {
            case ENG -> "The original festival or event has ended; we're recommending this nearby attraction instead.";
            case JPN -> "元の祭り・イベントは終了しているため、付近のこの観光地をご案内しています。";
            case CHS -> "原节日或活动已结束，现推荐此附近的景点作为替代。";
            case CHT -> "原節日或活動已結束，現推薦此附近的景點作為替代。";
            case KOR -> "종료된 축제·행사 대신, 근처 관광지로 안내했습니다.";
        };
    }

    private Map<Long, AttractionTranslation> loadAttractionTranslationsForItems(
            List<TourCourseItem> items,
            TourLanguage lang) {
        String code = attractionDbLangCode(lang);
        if ("ko".equals(code) || items == null || items.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = items.stream()
                .filter(i -> i.getItemType() == CourseItemType.ATTRACTION && i.getAttraction() != null)
                .map(i -> i.getAttraction().getId())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return attractionTranslationRepository.findByIdAttractionIdInAndIdLang(ids, code).stream()
                .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t, (a, b) -> a));
    }

    /**
     * 코스 스텝 {@code ai_comment} 번역. {@code tour_course_item_translations}에 행이 없으면 원문(한국어 컬럼) 사용.
     * CHT는 CHS 번역이 있으면 그걸로 폴백.
     */
    private Map<Long, String> loadItemAiCommentTranslations(List<TourCourseItem> items, TourLanguage lang) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = items.stream().map(TourCourseItem::getId).collect(Collectors.toList());
        Map<Long, String> out = new LinkedHashMap<>();
        if (lang != null && lang != TourLanguage.KOR) {
            for (TourCourseItemTranslation t : tourCourseItemTranslationRepository.findByCourseItem_IdInAndLanguage(ids, lang)) {
                if (t.getCourseItem() != null && t.getAiComment() != null && !t.getAiComment().isBlank()) {
                    out.put(t.getCourseItem().getId(), t.getAiComment().trim());
                }
            }
            if (lang == TourLanguage.CHT) {
                List<Long> missing = ids.stream().filter(id -> !out.containsKey(id)).collect(Collectors.toList());
                if (!missing.isEmpty()) {
                    for (TourCourseItemTranslation t : tourCourseItemTranslationRepository.findByCourseItem_IdInAndLanguage(
                            missing, TourLanguage.CHS)) {
                        if (t.getCourseItem() != null && t.getAiComment() != null && !t.getAiComment().isBlank()) {
                            out.putIfAbsent(t.getCourseItem().getId(), t.getAiComment().trim());
                        }
                    }
                }
            }
        }
        return out;
    }

    private static String resolveItemAiComment(TourCourseItem item, Map<Long, String> itemAiComments) {
        if (item == null) {
            return "";
        }
        if (itemAiComments != null) {
            String t = itemAiComments.get(item.getId());
            if (t != null && !t.isBlank()) {
                return t.trim();
            }
        }
        return item.getAiComment() != null ? item.getAiComment().trim() : "";
    }

    private List<TourCourseItemResponse> buildDetailItemsResolvingSpots(
            List<TourCourseItem> items,
            TourLanguage lang) {
        Map<Long, AttractionTranslation> attractionTranslations = loadAttractionTranslationsForItems(items, lang);
        Map<Long, String> itemAiComments = loadItemAiCommentTranslations(items, lang);
        String todayYmd = LocalDate.now(SEOUL_ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        Set<Long> usedAttractionIds = new LinkedHashSet<>();
        Set<Long> usedEventIds = new LinkedHashSet<>();
        Attraction lastResolvedAttraction = null;
        List<TourCourseItemResponse> out = new ArrayList<>();

        for (TourCourseItem item : items) {
            if (item.getItemType() == CourseItemType.EVENT) {
                TourApiEvent ev = item.getEvent();
                if (ev == null) {
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    continue;
                }
                if (!isEventExpiredByEndDate(ev, todayYmd)) {
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    usedEventIds.add(ev.getContentId());
                    continue;
                }

                Double mapX = ev.getMapX();
                Double mapY = ev.getMapY();
                if (mapX == null || mapY == null) {
                    log.debug(
                            "코스 상세: 종료된 행사인데 좌표 없음 sequenceOrder={} contentId={}",
                            item.getSequenceOrder(),
                            ev.getContentId());
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    continue;
                }

                List<Long> excludeEvents = new ArrayList<>(usedEventIds.size() + 1);
                excludeEvents.addAll(usedEventIds);
                excludeEvents.add(ev.getContentId());

                Optional<TourApiEvent> nearEvent = resolveNearestValidEvent(mapX, mapY, todayYmd, excludeEvents);
                if (nearEvent.isPresent()) {
                    TourApiEvent ne = nearEvent.get();
                    double d = haversineMeters(mapY, mapX, ne.getMapY(), ne.getMapX());
                    if (d <= MAX_EVENT_REPLACEMENT_DISTANCE_M) {
                        String merged = mergeAiComment(
                                resolveItemAiComment(item, itemAiComments), eventExpiredNearEventSuffix(lang));
                        out.add(buildEventReplacementResponse(item, ne, lang, merged));
                        usedEventIds.add(ne.getContentId());
                        continue;
                    }
                }

                List<Long> excludeAttr = new ArrayList<>(usedAttractionIds);
                Optional<Attraction> att = resolveNearbyReplacementAttraction(mapX, mapY, excludeAttr);
                if (att.isPresent()) {
                    Attraction a = att.get();
                    String comment = composeAiCommentForExpiredEventReplacedByAttraction(item, a, itemAiComments, lang);
                    out.add(buildAttractionReplacementResponse(item, a, comment, lang));
                    usedAttractionIds.add(a.getId());
                    lastResolvedAttraction = a;
                } else {
                    log.debug(
                            "코스 상세: 종료 행사 대체 실패 sequenceOrder={} contentId={}",
                            item.getSequenceOrder(),
                            ev.getContentId());
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                }
                continue;
            }

            if (item.getItemType() == CourseItemType.ATTRACTION) {
                Attraction at = item.getAttraction();
                if (at == null || at.getGeom() == null) {
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    continue;
                }

                double lat = at.getGeom().getY();
                double lon = at.getGeom().getX();
                Long curId = at.getId();

                boolean dup = usedAttractionIds.contains(curId);
                boolean tooClose = lastResolvedAttraction != null
                        && lastResolvedAttraction.getGeom() != null
                        && haversineMeters(
                                lastResolvedAttraction.getGeom().getY(),
                                lastResolvedAttraction.getGeom().getX(),
                                lat,
                                lon) < MIN_SPOT_SEPARATION_METERS;

                if (!dup && !tooClose) {
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    usedAttractionIds.add(curId);
                    lastResolvedAttraction = at;
                    continue;
                }

                List<Long> exclude = new ArrayList<>(usedAttractionIds.size() + 1);
                exclude.addAll(usedAttractionIds);
                exclude.add(curId);

                Optional<Attraction> replacement = resolveNearbyReplacementAttraction(lon, lat, exclude);
                if (replacement.isEmpty()) {
                    log.debug("코스 상세 겹침 대체: 후보 없음 sequenceOrder={} attractionId={}", item.getSequenceOrder(), curId);
                    out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
                    usedAttractionIds.add(curId);
                    lastResolvedAttraction = at;
                    continue;
                }

                Attraction rep = replacement.get();
                String merged = mergeAiComment(
                        resolveItemAiComment(item, itemAiComments), overlapReplacementCommentSuffix(lang));
                out.add(buildAttractionReplacementResponse(item, rep, merged, lang));
                usedAttractionIds.add(rep.getId());
                lastResolvedAttraction = rep;
                continue;
            }

            out.add(buildItemResponse(item, lang, attractionTranslations, itemAiComments));
        }

        return out;
    }

    private static boolean isEventExpiredByEndDate(TourApiEvent e, String todayYmd) {
        String end = e.getEventEndDate();
        if (end == null || end.isBlank()) {
            return false;
        }
        String end8 = end.length() > 8 ? end.substring(0, 8) : end;
        if (end8.length() != 8 || !end8.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return end8.compareTo(todayYmd) < 0;
    }

    private Optional<TourApiEvent> resolveNearestValidEvent(
            double lon,
            double lat,
            String todayYmd,
            List<Long> excludeIds) {
        List<Long> ids = tourApiEventRepository.findNearestValidEventContentIdExcluding(lon, lat, todayYmd, excludeIds);
        Long cid = firstLongId(ids);
        if (cid == null) {
            return Optional.empty();
        }
        return tourApiEventRepository.findById(cid).filter(e -> e.getMapX() != null && e.getMapY() != null);
    }

    private TourCourseItemResponse buildEventReplacementResponse(
            TourCourseItem item,
            TourApiEvent event,
            TourLanguage lang,
            String aiComment) {
        TourApiEventTranslation et = eventTranslationRepository.findByEventAndLanguage(event, lang)
                .orElseGet(() -> eventTranslationRepository.findByEventAndLanguage(event, TourLanguage.KOR).orElse(null));

        return TourCourseItemResponse.builder()
                .itemType(CourseItemType.EVENT)
                .sequenceOrder(item.getSequenceOrder())
                .aiComment(aiComment)
                .id(event.getContentId())
                .name(et != null ? et.getTitle() : "Event")
                .address(et != null ? et.getAddress() : "")
                .thumbnail(event.getFirstImage())
                .latitude(event.getMapY())
                .longitude(event.getMapX())
                .build();
    }

    private TourCourseItemResponse buildAttractionReplacementResponse(
            TourCourseItem item,
            Attraction rep,
            String mergedAiComment,
            TourLanguage lang) {
        String code = attractionDbLangCode(lang);
        AttractionTranslation tr = "ko".equals(code)
                ? null
                : attractionTranslationRepository.findByIdAttractionIdAndIdLang(rep.getId(), code).orElse(null);
        String name = firstNonBlank(
                tr != null ? tr.getName() : null,
                rep.getName());
        String address = firstNonBlank(
                tr != null ? tr.getAddress() : null,
                rep.getAddress());
        return TourCourseItemResponse.builder()
                .itemType(CourseItemType.ATTRACTION)
                .sequenceOrder(item.getSequenceOrder())
                .aiComment(mergedAiComment)
                .id(rep.getId())
                .name(name)
                .address(address)
                .thumbnail(rep.getThumbnail())
                .latitude(rep.getGeom().getY())
                .longitude(rep.getGeom().getX())
                .build();
    }

    private Optional<Attraction> resolveNearbyReplacementAttraction(double lon, double lat, List<Long> excludeIds) {
        List<Long> ids = attractionRepository.findNearestAttractionIdBeyondMetersExcluding(
                lon, lat, MIN_SPOT_SEPARATION_METERS, excludeIds);
        if (ids == null || ids.isEmpty()) {
            ids = attractionRepository.findNearestAttractionIdExcluding(lon, lat, excludeIds);
        }
        Long rid = firstLongId(ids);
        if (rid == null) {
            return Optional.empty();
        }
        return attractionRepository.findById(rid).filter(a -> a.getGeom() != null);
    }

    private static Long firstLongId(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.get(0);
    }

    private static String mergeAiComment(String existing, String suffix) {
        if (existing != null && !existing.isBlank()) {
            return existing.trim() + " · " + suffix;
        }
        return suffix;
    }

    /**
     * 종료된 행사 슬롯을 관광지로 채울 때 코멘트: 요청 언어의 {@code attraction_translation.overview} 우선,
     * 없으면 한국어 overview·코스 스텝 번역·카테고리 순. 모두 없으면 언어별 짧은 안내 문구.
     */
    private String composeAiCommentForExpiredEventReplacedByAttraction(
            TourCourseItem item,
            Attraction rep,
            Map<Long, String> itemAiComments,
            TourLanguage lang) {
        if (rep == null) {
            return eventExpiredReplacedByAttractionFallback(lang);
        }
        String code = attractionDbLangCode(lang);
        if (!"ko".equals(code)) {
            Optional<AttractionTranslation> tr = attractionTranslationRepository.findByIdAttractionIdAndIdLang(
                    rep.getId(), code);
            if (tr.isPresent()) {
                String ov = tr.get().getOverview();
                if (ov != null && !ov.isBlank()) {
                    return truncateForCourseStepComment(ov.trim());
                }
            }
        }
        String overview = rep.getOverview();
        if (overview != null && !overview.isBlank()) {
            return truncateForCourseStepComment(overview.trim());
        }
        String base = resolveItemAiComment(item, itemAiComments);
        if (!base.isBlank()) {
            return base;
        }
        if (rep.getCategory() != null && !rep.getCategory().isBlank()) {
            return rep.getCategory().trim();
        }
        return eventExpiredReplacedByAttractionFallback(lang);
    }

    private static String truncateForCourseStepComment(String text) {
        if (text.length() <= COURSE_SPOT_COMMENT_MAX_CHARS) {
            return text;
        }
        return text.substring(0, COURSE_SPOT_COMMENT_MAX_CHARS).trim() + "…";
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback != null ? fallback.trim() : "";
    }

    private TourCourseItemResponse buildItemResponse(
            TourCourseItem item,
            TourLanguage lang,
            Map<Long, AttractionTranslation> attractionTranslations,
            Map<Long, String> itemAiComments) {
        TourCourseItemResponse.TourCourseItemResponseBuilder builder = TourCourseItemResponse.builder()
                .itemType(item.getItemType())
                .sequenceOrder(item.getSequenceOrder())
                .aiComment(resolveItemAiComment(item, itemAiComments));

        if (item.getItemType() == CourseItemType.ATTRACTION && item.getAttraction() != null) {
            Attraction a = item.getAttraction();
            AttractionTranslation tr = attractionTranslations != null
                    ? attractionTranslations.get(a.getId())
                    : null;
            String name = firstNonBlank(tr != null ? tr.getName() : null, a.getName());
            String address = firstNonBlank(tr != null ? tr.getAddress() : null, a.getAddress());
            builder.id(a.getId())
                    .name(name)
                    .address(address)
                    .thumbnail(a.getThumbnail())
                    .latitude(a.getGeom().getY())
                    .longitude(a.getGeom().getX());
        } else if (item.getItemType() == CourseItemType.EVENT && item.getEvent() != null) {
            TourApiEvent event = item.getEvent();
            // 이벤트는 다국어 번역본 적용
            TourApiEventTranslation et = eventTranslationRepository.findByEventAndLanguage(event, lang)
                    .orElseGet(() -> eventTranslationRepository.findByEventAndLanguage(event, TourLanguage.KOR).orElse(null));

            builder.id(event.getContentId())
                    .name(et != null ? et.getTitle() : "Event")
                    .address(et != null ? et.getAddress() : "")
                    .thumbnail(event.getFirstImage())
                    .latitude(event.getMapY())
                    .longitude(event.getMapX());
        }

        return builder.build();
    }
}
