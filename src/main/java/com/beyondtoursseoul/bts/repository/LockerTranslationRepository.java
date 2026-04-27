package com.beyondtoursseoul.bts.repository;

import com.beyondtoursseoul.bts.domain.Locker;
import com.beyondtoursseoul.bts.domain.LockerTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 물품보관함 번역 Repository
 * */
@Repository
public interface LockerTranslationRepository extends JpaRepository<LockerTranslation, Long> {
    List<LockerTranslation> findByLanguageCode(String languageCode);

    /// 언어와 락커 두개를 사용해서 유일하게 넘겨주기
    Optional<LockerTranslation> findByLockerAndLanguageCode(Locker locker, String languageCode);
}
