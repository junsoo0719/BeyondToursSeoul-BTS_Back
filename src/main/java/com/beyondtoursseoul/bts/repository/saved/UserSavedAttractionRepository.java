package com.beyondtoursseoul.bts.repository.saved;

import com.beyondtoursseoul.bts.domain.Attraction;
import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedAttraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSavedAttractionRepository extends JpaRepository<UserSavedAttraction, Long> {

    List<UserSavedAttraction> findByUserOrderBySavedAtDesc(Profile user);

    Optional<UserSavedAttraction> findByUserAndAttraction(Profile user, Attraction attraction);

    boolean existsByUserAndAttraction(Profile user, Attraction attraction);
}
