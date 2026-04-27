package com.beyondtoursseoul.bts.service.translation;

import java.util.List;
import java.util.Map;

/**
 * 물품보관함 번역에 사용되는 고정 메시지 및 설정 상수 클래스
 */
public class LockerTranslationConstants {

    public static final List<String> TARGET_LANGS = List.of("en", "zh", "ja");
    public static final int CHUNK_SIZE = 3;
    public static final int FIELDS_PER_LOCKER = 4;

    // 보관 제한 물품 안내 문구
    public static final Map<String, String> LIMIT_ITEMS_MESSAGES = Map.of(
            "ko", "귀중품(현금/보석), 위험물(폭발물/흉기/마약), 부패성 식품, 동물, 대형화물(30kg 이상) 등 안전 저해 물품은 보관이 금지됩니다.",
            "en", "Storage of valuables, hazardous materials (explosives/weapons/drugs), perishable food, animals, and bulky items (over 30kg) is prohibited.",
            "zh", "禁止保管贵重物品、危险品（爆炸物/武器/毒品）、易腐食品、动物以及大件物品（超过30公斤）。",
            "ja", "貴重品、危険物（爆発物/凶器/麻薬）、腐敗성食品、動物、大型荷物（30kg以上）などの保管は禁止されています。"
    );

    // 기본 요금 정보
    public static final Map<String, String> BASE_PRICE_MESSAGES = Map.of(
            "ko", "기본 4시간: [평일] 소:2200/중:3300/대:4400 [주말] 소:3100/중:4600/대:6100",
            "en", "Base 4h: [Weekdays] S:2200/M:3300/L:4400 [Weekends] S:3100/M:4600/L:6100",
            "zh", "基本 4小时：[平日] 小:2200/中:3300/大:4400 [周末] 小:3100/中:4600/大:6100",
            "ja", "基本 4時間：[平日] 小:2200/중:3300/大:4400 [週末] 小:3100/중:4600/大:6100"
    );

    // 추가 요금 정보
    public static final Map<String, String> ADD_PRICE_MESSAGES = Map.of(
            "ko", "4시간 초과 시 1시간당 추가 요금: 소형 500원, 중형 800원, 대형 1,000원",
            "en", "After 4h, extra hourly charge: S 500, M 800, L 1,000 KRW",
            "zh", "超过4小时后，每小时加收：小 500, 中 800, 大 1,000韩元",
            "ja", "4時間超過後、1時間毎に追加：小 500, 중 800, 大 1,000ウォン"
    );
}
