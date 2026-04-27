package com.beyondtoursseoul.bts.service.translation;

import com.beyondtoursseoul.bts.domain.LockerTranslation;
import com.beyondtoursseoul.bts.repository.LockerTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 물품보관함 번역기 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class LockerTranslationBatchTranslator {

    private final TranslationService translationService;
    private final LockerTranslationRepository lockerTranslationRepository;

    private static final List<String> TARGET_LANGS = List.of("en", "zh", "ja");

    // 보관 제한 물품 안내 문구 요약 및 상수화
    private static final Map<String, String> LIMIT_ITEMS_MESSAGES = Map.of(
            "ko", "귀중품(현금/보석), 위험물(폭발물/흉기/마약), 부패성 식품, 동물, 대형화물(30kg 이상) 등 안전 저해 물품은 보관이 금지됩니다.",
            "en", "Storage of valuables, hazardous materials (explosives/weapons/drugs), perishable food, animals, and bulky items (over 30kg) is prohibited.",
            "zh", "禁止保管贵重物品、危险品（爆炸物/武器/毒品）、易腐食品、动物以及大件物品（超过30公斤）。",
            "ja", "貴重品、危険物（爆発物/凶器/麻薬）、腐敗性食品、動物、大型荷物（30kg以上）などの保管は禁止されています。"
    );

    @Transactional
    public void translateAllKoToMultiLang() {
        List<LockerTranslation> koTranslations = lockerTranslationRepository.findByLanguageCode("ko");

        for (LockerTranslation ko : koTranslations) {
            for (String lang : TARGET_LANGS) {
                Optional<LockerTranslation> existing = lockerTranslationRepository.findByLockerAndLanguageCode(ko.getLocker(), lang);
                LockerTranslation translated = buildTranslated(ko, lang);
                
                if (existing.isPresent()) {
                    LockerTranslation target = existing.get();
                    target.update(
                            translated.getStationName(),
                            translated.getLockerName(),
                            translated.getDetailLocation(),
                            translated.getBasePriceInfo(),
                            translated.getAddPriceInfo(),
                            translated.getSizeInfo(),
                            translated.getLimitItemsInfo()
                    );
                    lockerTranslationRepository.save(target);
                } else {
                    lockerTranslationRepository.save(translated);
                }
            }
        }
    }

    private LockerTranslation buildTranslated(LockerTranslation source, String lang) {
        return LockerTranslation.builder()
                .locker(source.getLocker())
                .languageCode(lang)
                .stationName(translationService.translate(source.getStationName(), "ko", lang))
                .lockerName(translationService.translate(source.getLockerName(), "ko", lang))
                .detailLocation(translationService.translate(source.getDetailLocation(), "ko", lang))
                .basePriceInfo(translationService.translate(source.getBasePriceInfo(), "ko", lang))
                .addPriceInfo(translationService.translate(source.getAddPriceInfo(), "ko", lang))
                .sizeInfo(translationService.translate(source.getSizeInfo(), "ko", lang))
                // 상수 맵에서 해당 언어의 요약 문구를 가져옴 (없으면 원본 유지)
                .limitItemsInfo(LIMIT_ITEMS_MESSAGES.getOrDefault(lang, source.getLimitItemsInfo()))
                .build();
    }
}
