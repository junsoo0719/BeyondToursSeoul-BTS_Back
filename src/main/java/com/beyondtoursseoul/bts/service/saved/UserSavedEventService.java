package com.beyondtoursseoul.bts.service.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEventTranslation;
import com.beyondtoursseoul.bts.domain.tour.TourLanguage;
import com.beyondtoursseoul.bts.dto.saved.SavedEventResponse;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.saved.UserSavedEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventRepository;
import com.beyondtoursseoul.bts.repository.tour.TourApiEventTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSavedEventService {

    private final UserSavedEventRepository userSavedEventRepository;
    private final ProfileRepository profileRepository;
    private final TourApiEventRepository eventRepository;
    private final TourApiEventTranslationRepository translationRepository;

    @Transactional(readOnly = true)
    public List<SavedEventResponse> listSaved(UUID userId, String acceptLanguage) {
        Profile user = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(Profile.createForUser(userId)));
        List<UserSavedEvent> rows = userSavedEventRepository.findByUserOrderBySavedAtDesc(user);
        if (rows.isEmpty()) {
            return List.of();
        }
        TourLanguage preferred = resolveTourLanguage(acceptLanguage);
        List<TourApiEvent> events = rows.stream()
                .map(UserSavedEvent::getEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(TourApiEvent::getContentId, e -> e, (a, b) -> a, HashMap::new),
                        m -> new ArrayList<>(m.values())));
        List<TourLanguage> languages = Arrays.asList(TourLanguage.values());
        List<TourApiEventTranslation> translations = events.isEmpty()
                ? List.of()
                : translationRepository.findByEventInAndLanguageIn(events, languages);
        Map<Long, EnumMap<TourLanguage, TourApiEventTranslation>> byContentId = new HashMap<>();
        for (TourApiEventTranslation tr : translations) {
            if (tr.getEvent() == null || tr.getLanguage() == null) {
                continue;
            }
            Long cid = tr.getEvent().getContentId();
            byContentId.computeIfAbsent(cid, k -> new EnumMap<>(TourLanguage.class)).put(tr.getLanguage(), tr);
        }
        return rows.stream()
                .map(saved -> {
                    TourApiEvent event = saved.getEvent();
                    if (event == null) {
                        return null;
                    }
                    EnumMap<TourLanguage, TourApiEventTranslation> perLang = byContentId.get(event.getContentId());
                    TourApiEventTranslation tr = pickTranslation(perLang, preferred);
                    return toResponse(event, tr, saved.getSavedAt());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean toggleSave(Long eventContentId, UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(Profile.createForUser(userId)));
        TourApiEvent event = eventRepository.findById(eventContentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행사입니다."));

        return userSavedEventRepository.findByUserAndEvent(user, event)
                .map(existing -> {
                    userSavedEventRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    userSavedEventRepository.save(UserSavedEvent.builder()
                            .user(user)
                            .event(event)
                            .build());
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean isSaved(Long eventContentId, UUID userId) {
        Profile user = profileRepository.findById(userId).orElse(null);
        if (user == null) return false;
        TourApiEvent event = eventRepository.findById(eventContentId).orElse(null);
        if (event == null) return false;
        return userSavedEventRepository.existsByUserAndEvent(user, event);
    }

    private SavedEventResponse toResponse(
            TourApiEvent event,
            TourApiEventTranslation tr,
            java.time.OffsetDateTime savedAt
    ) {
        String title = tr != null && tr.getTitle() != null ? tr.getTitle().trim() : "";
        String address = addressFromTranslation(tr);
        return SavedEventResponse.builder()
                .contentId(event.getContentId())
                .title(title)
                .address(address)
                .firstImage(event.getFirstImage())
                .eventStartDate(event.getEventStartDate())
                .eventEndDate(event.getEventEndDate())
                .mapX(event.getMapX())
                .mapY(event.getMapY())
                .savedAt(savedAt)
                .build();
    }

    /** {@link UserSavedAttractionService} 와 동일한 Accept-Language 1차 토큰 규칙 */
    private static String normalizeLang(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "ko";
        }
        return acceptLanguage.split("[,;\\-]")[0].trim().toLowerCase();
    }

    private static TourLanguage resolveTourLanguage(String acceptLanguage) {
        return TourLanguage.fromCode(normalizeLang(acceptLanguage));
    }

    private static TourApiEventTranslation pickTranslation(
            EnumMap<TourLanguage, TourApiEventTranslation> perLang,
            TourLanguage preferred
    ) {
        if (perLang == null || perLang.isEmpty()) {
            return null;
        }
        TourApiEventTranslation tr = perLang.get(preferred);
        if (tr != null) {
            return tr;
        }
        for (TourLanguage fb : List.of(
                TourLanguage.KOR,
                TourLanguage.ENG,
                TourLanguage.JPN,
                TourLanguage.CHS,
                TourLanguage.CHT
        )) {
            tr = perLang.get(fb);
            if (tr != null) {
                return tr;
            }
        }
        return perLang.values().iterator().next();
    }

    private static String addressFromTranslation(TourApiEventTranslation tr) {
        if (tr == null) {
            return "";
        }
        String a = tr.getAddress();
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        String p = tr.getEventPlace();
        return p != null ? p.trim() : "";
    }
}
