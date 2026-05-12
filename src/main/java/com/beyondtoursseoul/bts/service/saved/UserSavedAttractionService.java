package com.beyondtoursseoul.bts.service.saved;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.AttractionTranslation;
import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedAttraction;
import com.beyondtoursseoul.bts.dto.saved.SavedAttractionResponse;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.AttractionTranslationRepository;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.saved.UserSavedAttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSavedAttractionService {

    private final UserSavedAttractionRepository userSavedAttractionRepository;
    private final ProfileRepository profileRepository;
    private final AttractionRepository attractionRepository;
    private final AttractionTranslationRepository attractionTranslationRepository;

    @Transactional(readOnly = true)
    public List<SavedAttractionResponse> listSaved(UUID userId, String acceptLanguage) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<UserSavedAttraction> rows = userSavedAttractionRepository.findByUserOrderBySavedAtDesc(user);
        if (rows.isEmpty()) {
            return List.of();
        }
        String lang = normalizeLang(acceptLanguage);
        List<Long> ids = rows.stream()
                .map(s -> s.getAttraction().getId())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AttractionTranslation> translationMap = isKorean(lang) || ids.isEmpty()
                ? Map.of()
                : attractionTranslationRepository.findByIdAttractionIdInAndIdLang(ids, lang).stream()
                .collect(Collectors.toMap(t -> t.getId().getAttractionId(), t -> t, (a, b) -> a));
        return rows.stream()
                .map(s -> {
                    Attraction a = s.getAttraction();
                    AttractionTranslation tr = translationMap.get(a.getId());
                    return new SavedAttractionResponse(a, tr, s.getSavedAt());
                })
                .collect(Collectors.toList());
    }

    private static boolean isKorean(String lang) {
        return lang == null || lang.isBlank() || lang.toLowerCase().startsWith("ko");
    }

    /** {@link com.beyondtoursseoul.bts.controller.AttractionController} 와 동일한 1차 토큰 규칙 */
    private static String normalizeLang(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "ko";
        }
        return acceptLanguage.split("[,;\\-]")[0].trim().toLowerCase();
    }

    @Transactional
    public boolean toggleSave(Long attractionId, UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관광지입니다."));

        Optional<UserSavedAttraction> existing = userSavedAttractionRepository.findByUserAndAttraction(user, attraction);
        if (existing.isPresent()) {
            userSavedAttractionRepository.delete(existing.get());
            return false;
        }
        userSavedAttractionRepository.save(UserSavedAttraction.builder()
                .user(user)
                .attraction(attraction)
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isSaved(Long attractionId, UUID userId) {
        Profile user = profileRepository.findById(userId).orElse(null);
        if (user == null) return false;
        Attraction attraction = attractionRepository.findById(attractionId).orElse(null);
        if (attraction == null) return false;
        return userSavedAttractionRepository.existsByUserAndAttraction(user, attraction);
    }
}
