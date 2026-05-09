package com.beyondtoursseoul.bts.repository.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSavedPlanRepository extends JpaRepository<UserSavedPlan, Long> {

    List<UserSavedPlan> findByUserOrderBySavedAtDesc(Profile user);

    Optional<UserSavedPlan> findByIdAndUser(Long id, Profile user);
}
