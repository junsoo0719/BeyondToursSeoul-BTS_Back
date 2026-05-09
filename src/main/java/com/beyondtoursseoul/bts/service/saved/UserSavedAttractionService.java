package com.beyondtoursseoul.bts.service.saved;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedAttraction;
import com.beyondtoursseoul.bts.dto.saved.SavedAttractionResponse;
import com.beyondtoursseoul.bts.repository.AttractionRepository;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.beyondtoursseoul.bts.repository.saved.UserSavedAttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSavedAttractionService {

    private final UserSavedAttractionRepository userSavedAttractionRepository;
    private final ProfileRepository profileRepository;
    private final AttractionRepository attractionRepository;

    @Transactional(readOnly = true)
    public List<SavedAttractionResponse> listSaved(UUID userId) {
        Profile user = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return userSavedAttractionRepository.findByUserOrderBySavedAtDesc(user).stream()
                .map(s -> new SavedAttractionResponse(s.getAttraction(), s.getSavedAt()))
                .collect(Collectors.toList());
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
