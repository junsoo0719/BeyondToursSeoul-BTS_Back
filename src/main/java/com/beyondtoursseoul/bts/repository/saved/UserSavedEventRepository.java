package com.beyondtoursseoul.bts.repository.saved;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.domain.saved.UserSavedEvent;
import com.beyondtoursseoul.bts.domain.tour.TourApiEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSavedEventRepository extends JpaRepository<UserSavedEvent, Long> {

    List<UserSavedEvent> findByUserOrderBySavedAtDesc(Profile user);

    Optional<UserSavedEvent> findByUserAndEvent(Profile user, TourApiEvent event);

    boolean existsByUserAndEvent(Profile user, TourApiEvent event);
}
