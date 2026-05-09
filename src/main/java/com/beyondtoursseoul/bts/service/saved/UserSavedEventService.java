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

import java.util.List;
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
    public List<SavedEventResponse> listSaved(UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(Profile.createForUser(userId)));
        return userSavedEventRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(saved -> toResponse(saved.getEvent(), saved.getSavedAt()))
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

    private SavedEventResponse toResponse(TourApiEvent event, java.time.OffsetDateTime savedAt) {
        TourApiEventTranslation tr = translationRepository.findByEventAndLanguage(event, TourLanguage.KOR)
                .orElseGet(() -> translationRepository.findByEventAndLanguage(event, TourLanguage.ENG).orElse(null));
        return SavedEventResponse.builder()
                .contentId(event.getContentId())
                .title(tr != null ? tr.getTitle() : "")
                .address(tr != null ? tr.getAddress() : "")
                .firstImage(event.getFirstImage())
                .eventStartDate(event.getEventStartDate())
                .eventEndDate(event.getEventEndDate())
                .mapX(event.getMapX())
                .mapY(event.getMapY())
                .savedAt(savedAt)
                .build();
    }
}
