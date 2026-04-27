package com.beyondtoursseoul.bts.repository.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    // 락커 id로 조회
    Optional<Locker> findByLckrId(String lckrId);
}
