package com.beyondtoursseoul.bts.service.course;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.course.*;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.course.TourCourseDetailResponse;
import com.beyondtoursseoul.bts.dto.course.TourCourseItemResponse;
import com.beyondtoursseoul.bts.dto.course.TourCourseSummaryResponse;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseItemRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseRepository;
import com.beyondtoursseoul.bts.repository.course.TourCourseTranslationRepository;
import com.beyondtoursseoul.bts.repository.course.UserSavedCourseRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCourseService {

    private final TourCourseRepository tourCourseRepository;
    private final TourCourseItemRepository tourCourseItemRepository;
    private final TourCourseTranslationRepository tourCourseTranslationRepository;
    private final UserSavedCourseRepository userSavedCourseRepository;
    private final ProfileRepository profileRepository;
    private final TourApiEventTranslationRepository eventTranslationRepository;

    /**
     * 추천 여행 코스 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TourCourseSummaryResponse> getCourseList(TourLanguage lang, UUID userId) {
        Profile user = userId != null ? profileRepository.findById(userId).orElse(null) : null;

        return tourCourseRepository.findAll().stream()
                .map(course -> {
                    TourCourseTranslation translation = getTranslation(course, lang);
                    boolean isSaved = user != null && userSavedCourseRepository.existsByUserAndCourse(user, course);

                    return TourCourseSummaryResponse.builder()
                            .id(course.getId())
                            .title(translation.getTitle())
                            .hashtags(translation.getHashtags())
                            .featuredImage(course.getFeaturedImage())
                            .isSaved(isSaved)
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

        List<TourCourseItem> items = tourCourseItemRepository.findByCourseOrderBySequenceOrderAsc(course);
        List<TourCourseItemResponse> itemResponses = items.stream()
                .map(item -> buildItemResponse(item, lang))
                .collect(Collectors.toList());

        return TourCourseDetailResponse.builder()
                .id(course.getId())
                .title(translation.getTitle())
                .hashtags(translation.getHashtags())
                .featuredImage(course.getFeaturedImage())
                .isSaved(isSaved)
                .items(itemResponses)
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

        return userSavedCourseRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(saved -> {
                    TourCourse course = saved.getCourse();
                    TourCourseTranslation translation = getTranslation(course, lang);
                    return TourCourseSummaryResponse.builder()
                            .id(course.getId())
                            .title(translation.getTitle())
                            .hashtags(translation.getHashtags())
                            .featuredImage(course.getFeaturedImage())
                            .isSaved(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private TourCourseTranslation getTranslation(TourCourse course, TourLanguage lang) {
        return tourCourseTranslationRepository.findByCourseAndLanguage(course, lang)
                .orElseGet(() -> tourCourseTranslationRepository.findByCourseAndLanguage(course, TourLanguage.KOR)
                        .orElse(TourCourseTranslation.builder()
                                .title(course.getTitle())
                                .hashtags(course.getHashtags())
                                .build()));
    }

    private TourCourseItemResponse buildItemResponse(TourCourseItem item, TourLanguage lang) {
        TourCourseItemResponse.TourCourseItemResponseBuilder builder = TourCourseItemResponse.builder()
                .itemType(item.getItemType())
                .sequenceOrder(item.getSequenceOrder())
                .aiComment(item.getAiComment());

        if (item.getItemType() == CourseItemType.ATTRACTION && item.getAttraction() != null) {
            builder.id(item.getAttraction().getId())
                    .name(item.getAttraction().getName())
                    .address(item.getAttraction().getAddress())
                    .thumbnail(item.getAttraction().getThumbnail())
                    .latitude(item.getAttraction().getGeom().getY())
                    .longitude(item.getAttraction().getGeom().getX());
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
