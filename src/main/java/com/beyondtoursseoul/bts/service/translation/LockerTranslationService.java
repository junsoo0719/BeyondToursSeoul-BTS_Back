package com.beyondtoursseoul.bts.service.translation;

import com.beyondtoursseoul.bts.domain.locker.LockerTranslation;
import com.beyondtoursseoul.bts.repository.locker.LockerTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.beyondtoursseoul.bts.service.translation.LockerTranslationConstants.*;

/**
 * 물품보관함 번역기 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockerTranslationService {

    private final TranslationService translationService;
    private final LockerTranslationRepository lockerTranslationRepository;

    @Transactional
    public void translateAllKoToMultiLang() {
        List<LockerTranslation> koTranslations = lockerTranslationRepository.findByLanguageCode("ko");
        log.info("=== 번역 배치 작업 시작 (대상: {}개) ===", koTranslations.size());

        for (String lang : TARGET_LANGS) {
            log.info("▶ [{}] 언어 작업 시작...", lang);
            
            int i = 0;
            int chunkRetryCount = 0;
            int maxRetries = 3;

            while (i < koTranslations.size()) {
                int end = Math.min(i + CHUNK_SIZE, koTranslations.size());
                List<LockerTranslation> chunk = koTranslations.subList(i, end);

                // 이미 번역된 데이터 건너뛰기
                if (isAllAlreadyTranslated(chunk, lang)) {
                    log.info("{}번 ~ {}번 보관함은 이미 번역되어 건너뜁니다.", i, end - 1);
                    i += CHUNK_SIZE;
                    chunkRetryCount = 0;
                    continue;
                }

                log.info("[{}] 번역 요청: {}번 ~ {}번 (재시도: {}/{})", 
                         lang, i, end - 1, chunkRetryCount, maxRetries);

                boolean success = processChunk(chunk, lang);

                if (!success) {
                    chunkRetryCount++;
                    if (chunkRetryCount >= maxRetries) {
                        log.error("{}번~{}번 청크가 {}회 연속 실패하여 이번 회차는 건너뜁니다.", i, end - 1, maxRetries);
                        i += CHUNK_SIZE;
                        chunkRetryCount = 0;
                        continue;
                    }

                    log.warn("할당량 초과 또는 API 오류! 30초 휴식 후 다시 시도합니다... ({}회차)", chunkRetryCount);
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("번역 중단됨: {}", e.getMessage());
                    }
                } else {
                    i += CHUNK_SIZE;
                    chunkRetryCount = 0;
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("번역 중단됨: {}", e.getMessage());
                    }
                }
            }
        }
        log.info("=== 번역 배치 작업 종료 ===");
    }

    private boolean isAllAlreadyTranslated(List<LockerTranslation> chunk, String lang) {
        for (LockerTranslation ko : chunk) {
            Optional<LockerTranslation> existing = lockerTranslationRepository
                    .findByLockerAndLanguageCode(ko.getLocker(), lang);
            
            if (existing.isEmpty() || existing.get().getLockerName().equals(ko.getLockerName())) {
                return false;
            }
        }
        return true;
    }

    private boolean processChunk(List<LockerTranslation> chunk, String lang) {
        List<String> allSourceTexts = new ArrayList<>();
        for (LockerTranslation ko : chunk) {
            allSourceTexts.addAll(List.of(
                    ko.getStationName(),
                    ko.getLockerName(),
                    ko.getDetailLocation(),
                    ko.getSizeInfo()
            ));
        }

        List<String> allTranslatedTexts = translationService.translateBatch(allSourceTexts, "ko", lang);

        if (allTranslatedTexts.isEmpty() || allTranslatedTexts.size() != allSourceTexts.size()) {
            return false;
        }

        for (int j = 0; j < chunk.size(); j++) {
            int startIndex = j * FIELDS_PER_LOCKER;
            List<String> translatedFields = allTranslatedTexts.subList(startIndex, startIndex + FIELDS_PER_LOCKER);
            saveOrUpdate(chunk.get(j), lang, translatedFields);
        }
        
        return true;
    }

    private void saveOrUpdate(LockerTranslation koSource, String lang, List<String> translatedFields) {
        Optional<LockerTranslation> existing = lockerTranslationRepository.findByLockerAndLanguageCode(koSource.getLocker(), lang);

        String basePrice = BASE_PRICE_MESSAGES.getOrDefault(lang, koSource.getBasePriceInfo());
        String addPrice = ADD_PRICE_MESSAGES.getOrDefault(lang, koSource.getAddPriceInfo());
        String limitItems = LIMIT_ITEMS_MESSAGES.getOrDefault(lang, koSource.getLimitItemsInfo());

        if (existing.isPresent()) {
            LockerTranslation target = existing.get();
            target.update(
                    translatedFields.get(0),
                    translatedFields.get(1),
                    translatedFields.get(2),
                    basePrice,
                    addPrice,
                    translatedFields.get(3),
                    limitItems
            );
            lockerTranslationRepository.save(target);
        } else {
            LockerTranslation newTranslation = LockerTranslation.builder()
                    .locker(koSource.getLocker())
                    .languageCode(lang)
                    .stationName(translatedFields.get(0))
                    .lockerName(translatedFields.get(1))
                    .detailLocation(translatedFields.get(2))
                    .basePriceInfo(basePrice)
                    .addPriceInfo(addPrice)
                    .sizeInfo(translatedFields.get(3))
                    .limitItemsInfo(limitItems)
                    .build();
            lockerTranslationRepository.save(newTranslation);
        }
    }
}
