package com.beyondtoursseoul.bts.repository.locker;

import com.beyondtoursseoul.bts.domain.locker.Locker;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    // 락커 id로 조회
    Optional<Locker> findByLckrId(String lckrId);

    // N+1 문제 해결을 위해 번역 데이터까지 한 번에 JOIN FETCH 로 가져오는 커스텀 쿼리
    @EntityGraph(attributePaths = {"translations"})
    @Query("SELECT l FROM Locker l")
    List<Locker> findAllWithTranslations();
}
