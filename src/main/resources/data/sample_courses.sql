-- ====================================================================
-- AI 추천 여행 코스 샘플 데이터 (초기화 및 삽입)
-- 코스 스텝 ai_comment 번역: tour_course_item_translations (DDL은 DB.md 참고 후 1회 적용). 샘플에는 ENG/JPN/CHS 수동 번역 포함; CHT는 API에서 CHS 폴백.
-- ====================================================================

-- 기존 샘플 데이터가 있다면 삭제 (ID 관계상 자식 테이블부터 삭제)
-- 제목 변경 이력이 있어도 한 번에 지우도록 이전/현재 제목 모두 나열
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '서울의 과거와 현재: 고궁 밤산책',
  '예술과 휴식이 있는 하루', '역사의 장면, 고요한 산책: 박물관과 덕수궁',
  'MZ세대 힙플레이스 탐방기', '오늘 서울의 온도: 숲과 스트릿을 잇다',
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책',
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지',
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링',
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합',
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '서울의 과거와 현재: 고궁 밤산책',
  '예술과 휴식이 있는 하루', '역사의 장면, 고요한 산책: 박물관과 덕수궁',
  'MZ세대 힙플레이스 탐방기', '오늘 서울의 온도: 숲과 스트릿을 잇다',
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책',
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지',
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링',
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합',
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '서울의 과거와 현재: 고궁 밤산책',
  '예술과 휴식이 있는 하루', '역사의 장면, 고요한 산책: 박물관과 덕수궁',
  'MZ세대 힙플레이스 탐방기', '오늘 서울의 온도: 숲과 스트릿을 잇다',
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책',
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지',
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링',
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합',
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_courses WHERE title IN (
  '서울의 과거와 현재: 고궁 밤산책',
  '예술과 휴식이 있는 하루', '역사의 장면, 고요한 산책: 박물관과 덕수궁',
  'MZ세대 힙플레이스 탐방기', '오늘 서울의 온도: 숲과 스트릿을 잇다',
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책',
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지',
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링',
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합',
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
);

-- 1. 추천 코스 마스터 데이터 삽입 (tour_courses)
INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES 
('서울의 과거와 현재: 고궁 밤산책', '#고궁 #야경 #전통 #데이트', 'https://tong.visitkorea.or.kr/cms/resource/21/2616021_image2_1.jpg', NOW()),
('역사의 장면, 고요한 산책: 박물관과 덕수궁', '#미술관 #전시 #공원 #힐링', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW()),
('오늘 서울의 온도: 숲과 스트릿을 잇다', '#팝업스토어 #트렌디 #인생샷 #서울숲', 'https://tong.visitkorea.or.kr/cms/resource/46/2800046_image2_1.jpg', NOW());

-- 2. 다국어 정보 삽입 (영어 버전)
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Past and Present of Seoul: Palace Night Walk', '#Palace #NightView #Tradition #Date' FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Scenes of History, a Quiet Walk: Museum & Deoksugung', '#Museum #Exhibition #Park #Healing' FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Seoul Today''s Pulse: From Forest to Street', '#PopUpStore #Trendy #SeoulForest' FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', 'ソウルの過去と現在：宮殿のナイトウォーク', '#景福宮 #夜景 #伝統 #デート' FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '歴史の情景、静かな散策：博物館と徳寿宮', '#美術館 #展示 #公園 #癒し' FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '今日のソウルの温度：森とストリートをつなぐ', '#ポップアップ #トレンド #ソウルの森' FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '首尔的过去与现在：宫殿夜游', '#景福宫 #夜景 #传统 #约会' FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '历史场景，静谧漫步：博物馆与德寿宫', '#美术馆 #展览 #公园 #治愈' FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '今日首尔温度：连接森林与街道', '#快闪 #潮流 #首尔林' FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다'
ON CONFLICT (course_id, language) DO NOTHING;

-- 3. 코스별 상세 아이템(관광지/행사) 결합 삽입
-- [코스 1: 고궁 밤산책]
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES 
(
  (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1), 
  'ATTRACTION', 
  (SELECT id FROM attraction WHERE name LIKE '%경복궁%' LIMIT 1), 
  NULL, 1, '서울의 상징인 경복궁에서 조선시대의 웅장함을 느껴보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1), 
  'EVENT', 
  NULL, 
  (SELECT content_id FROM tour_api_event LIMIT 1), 
  2, '현재 고궁 근처에서 열리는 특별한 문화 행사에 참여하여 추억을 만드세요.'
);

-- [코스 2: 박물관·덕수궁]
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES 
(
  (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1), 
  'ATTRACTION', 
  (SELECT id FROM attraction WHERE name LIKE '%국립중앙박물관%' LIMIT 1), 
  NULL, 1, '한국의 역사와 예술을 한눈에 볼 수 있는 세계적인 규모의 박물관입니다.'
),
(
  (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1), 
  'ATTRACTION', 
  (SELECT id FROM attraction WHERE name LIKE '%덕수궁%' LIMIT 1), 
  NULL, 2, '고즈넉한 돌담길을 따라 걸으며 여유로운 오후를 즐겨보세요.'
);

-- [코스 3: 서울숲·스트릿]
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES 
(
  (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1), 
  'ATTRACTION', 
  (SELECT id FROM attraction WHERE name LIKE '%서울숲%' LIMIT 1), 
  NULL, 1, '도심 속 거대한 숲에서 피크닉과 산책을 즐기기 좋습니다.'
),
(
  (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1), 
  'EVENT', 
  NULL, 
  (SELECT content_id FROM tour_api_event OFFSET 1 LIMIT 1), 
  2, '주변에서 열리는 힙한 팝업 행사나 전시를 확인해보세요!'
);

-- 첫 3개 코스: 스텝 ai_comment 번역 (tour_course_item_translations DDL 선행 필요)
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'Feel the grandeur of the Joseon era at Gyeongbokgung, Seoul''s iconic palace.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', 'ソウルの象徴・景福宮で、朝鮮時代の荘厳さを体感してください。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '在景福宫感受朝鲜王朝的恢弘气势——首尔的象征。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;

INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'Join a special cultural event near the palaces and make a memory.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', '宮殿周辺で開催中の特別な文化行事に参加して、思い出をつくりましょう。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '参加古宫附近举办的特别文化活动，留下一段回忆。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '서울의 과거와 현재: 고궁 밤산책' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;

INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'A world-class museum where you can see Korean history and art in one visit.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', '韓国の歴史と美術を一度に見られる、世界的規模の博物館です。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '可在一座世界级博物馆里纵览韩国历史与艺术。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;

INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'Stroll the quiet stone-wall path at Deoksugung and enjoy a relaxed afternoon.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', '徳寿宮の石垣の小道をゆっくり歩き、穏やかな午後を過ごしましょう。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '沿着德寿宫静谧的石墙路漫步，享受悠闲午后。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '역사의 장면, 고요한 산책: 박물관과 덕수궁' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;

INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'A vast urban forest—great for picnics and walks in the heart of the city.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', '都心に広がる大きな森で、ピクニックや散歩に最適です。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '都市里的大片森林，适合野餐与散步。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 1
ON CONFLICT (course_item_id, language) DO NOTHING;

INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'ENG', 'Check out trendy pop-ups and exhibitions happening nearby.'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'JPN', '周辺で開催中のポップアップや展示もチェックしてみましょう。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment)
SELECT id, 'CHS', '看看周边正在举办的潮流快闪与展览。'
FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '오늘 서울의 온도: 숲과 스트릿을 잇다' LIMIT 1) AND sequence_order = 2
ON CONFLICT (course_item_id, language) DO NOTHING;

-- ====================================================================
-- 하루짜리 공식 추천 코스 3종: 전통 / 쇼핑 / 야경·한강
-- (재실행 시 동일 제목 코스만 교체)
-- ====================================================================
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책'
));
DELETE FROM tour_courses WHERE title IN (
  '전통문화 코스', '궁궐에서 골목까지: 한복 입은 하루',
  '현대쇼핑 코스', '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지',
  '야경&한강 코스', '불빛 따라, 강까지: 서울의 밤 산책'
);

INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES
('궁궐에서 골목까지: 한복 입은 하루', '#경복궁 #한복 #북촌 #인사동 #쌈지길 #전통문화', 'https://tong.visitkorea.or.kr/cms/resource/21/2616021_image2_1.jpg', NOW()),
('트렌드가 숨 쉬는 길: 홍대에서 코엑스까지', '#홍대 #강남 #코엑스 #별마당 #쇼핑 #MZ', 'https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg', NOW()),
('불빛 따라, 강까지: 서울의 밤 산책', '#야경 #N서울타워 #청계천 #DDP #한강 #로맨틱', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW());

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'From Palace to Alley: A Day in Hanbok', '#Gyeongbokgung #Hanbok #Bukchon #Insadong #Ssamziegil #Heritage' FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Where the Trend Breathes: Hongdae to COEX', '#Hongdae #Gangnam #COEX #StarfieldLibrary #Shopping #KTrend' FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Through the Lights to the River: A Seoul Night Walk', '#NSeoulTower #Cheonggyecheon #DDP #HanRiver #Nightscape #Romantic' FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '宮殿から路地まで：韓服で過ごす一日', '#景福宮 #韓服 #北村 #仁寺洞 #サムジギル #伝統文化' FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', 'トレンドが息づく道：弘大からCOEXまで', '#弘大 #江南 #COEX #星空図書館 #ショッピング #MZ' FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '光をたどって川へ：ソウルの夜散歩', '#夜景 #Nソウルタワー #清渓川 #DDP #漢江 #ロマンティック' FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '从宫殿到小巷：韩服一日', '#景福宫 #韩服 #北村 #仁寺洞 #森吉街 #传统文化' FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '潮流呼吸之路：弘大到COEX', '#弘大 #江南 #COEX #星空图书馆 #购物 #韩流' FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '循光至江：首尔夜游', '#夜景 #N首尔塔 #清溪川 #DDP #汉江 #浪漫' FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책'
ON CONFLICT (course_id, language) DO NOTHING;

-- [전통 하루] 경복궁 → 북촌한옥마을 → 인사동 → 쌈지길
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경복궁%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Gyeongbokgung%' LIMIT 1)
  ),
  NULL, 1,
  '경복궁에서 한복 체험으로 궁궐의 웅장함을 느끼며 하루를 시작해 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%북촌한옥%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%북촌 한옥%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%북촌%' LIMIT 1)
  ),
  NULL, 2,
  '한옥 골목을 거닐며 고즈넉한 정취와 사진 스폿을 즐겨 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%인사동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Insadong%' LIMIT 1)
  ),
  NULL, 3,
  '전통 찻집과 기념품 골목, 길거리 간식으로 외국인에게도 신선한 매력을 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%쌈지길%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%쌈지 길%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%인사동%' LIMIT 1)
  ),
  NULL, 4,
  '쌈지길의 입체적인 골목과 독특한 상점들로 전통 코스의 피날레를 장식해 보세요.'
);

-- [쇼핑 하루] 홍대 → 트릭아트 뮤지엄 → 강남역 일대 → 코엑스·별마당 도서관
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%홍익대학교%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대거리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대%' LIMIT 1)
  ),
  NULL, 1,
  '홍대에서 버스킹과 거리 분위기를 즐기고, 트렌디한 카페 탐방으로 분위기를 이어 가 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%트릭아이%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%트릭아트%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Trick%' LIMIT 1)
  ),
  NULL, 2,
  '착시와 포토존이 가득한 트릭아트 뮤지엄에서 인생샷을 남겨 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%강남역%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%강남%' AND name ILIKE '%역%' LIMIT 1)
  ),
  NULL, 3,
  '강남역 지하상가와 주변 몰에서 VR·팝업 등 젊은 감성의 쇼핑과 체험을 이어 가 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%별마당%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%코엑스%' AND name ILIKE '%도서%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%코엑스%' LIMIT 1)
  ),
  NULL, 4,
  '코엑스와 별마당 도서관에서 트렌디한 쇼핑과 휴식을 한 번에 즐겨 보세요.'
);

-- [야경·한강] 남산 N서울타워 → 청계천 → 동대문 DDP → 한강공원
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%N서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%서울타워%' LIMIT 1)
  ),
  NULL, 1,
  '케이블카 또는 산책로로 올라 남산에서 도시 야경을 감상해 로맨틱한 밤을 열어 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%청계천%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Cheonggyecheon%' LIMIT 1)
  ),
  NULL, 2,
  '청계천을 따라 걸으며 야경과 산책로의 여유를 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%동대문디자인플라자%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%디자인플라자%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%DDP%' LIMIT 1)
  ),
  NULL, 3,
  'DDP의 미래지향적인 건축과 야간 조명이 도시의 밤을 완성합니다.'
),
(
  (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%반포한강공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%여의도한강공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%뚝섬한강공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%이촌한강공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%한강공원%' LIMIT 1)
  ),
  NULL, 4,
  '한강공원에서 강바람과 산책으로 하루를 여유롭게 마무리해 보세요.'
);

-- ====================================================================
-- 로컬 맛·힙 골목 코스 3종: 종로 / 남산·경리단 / 홍대·망원
-- ====================================================================
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
));
DELETE FROM tour_courses WHERE title IN (
  '종로 역사 코스', '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛',
  '남산 현대 코스', '골목의 온도, 불빛 위로: 경리단과 남산',
  '홍대 힙스터 코스', '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
);

INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES
('궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛', '#경복궁 #광장시장 #세종마을 #종로 #길거리음식 #한식', 'https://tong.visitkorea.or.kr/cms/resource/21/2616021_image2_1.jpg', NOW()),
('골목의 온도, 불빛 위로: 경리단과 남산', '#경리단길 #남산 #N서울타워 #이태원 #카페 #야경', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW()),
('거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지', '#홍대 #연남동 #망원시장 #버스킹 #떡볶이 #로컬', 'https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg', NOW());

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Around the Palace, Around the Skillet: Jongno History & Bites', '#Gyeongbokgung #GwangjangMarket #SejongVillage #Jongno #StreetFood #KoreanFood' FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Alley Warmth, City Lights Above: Gyeongridan & Namsan', '#Gyeongridan #Namsan #NSeoulTower #Itaewon #Cafe #NightView' FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Street Rhythms, Market Heat: Hongdae to Mangwon', '#Hongdae #Yeonnamdong #MangwonMarket #Busking #Tteokbokki #LocalVibes' FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '宮殿を一周、鍋を一皿：鐘路の歴史と味', '#景福宮 #広蔵市場 #世宗村 #鐘路 #屋台 #韓食' FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '路地の温度、光の上へ：経理団と南山', '#経理団路 #南山 #Nソウルタワー #梨泰院 #カフェ #夜景' FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '街のリズム、市場の熱さ：弘大から望遠まで', '#弘大 #延南洞 #望遠市場 #バスキング #トッポッキ #ローカル' FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '宫殿绕一圈，铁锅盛一碗：钟路历史与味道', '#景福宫 #广藏市场 #世宗村 #钟路 #街头小吃 #韩餐' FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '小巷温度，灯火之上：理贤洞与南山', '#理贤洞路 #南山 #N首尔塔 #梨泰院 #咖啡 #夜景' FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '街头节奏，市场热度：弘大到望远', '#弘大 #延南洞 #望远市场 #路演 #炒年糕 #本地氛围' FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지'
ON CONFLICT (course_id, language) DO NOTHING;

-- [종로] 경복궁 → 광장시장 → 세종마을 음식거리 일대
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경복궁%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Gyeongbokgung%' LIMIT 1)
  ),
  NULL, 1,
  '경복궁 관람으로 조선의 역사를 먼저 담은 뒤, 곧바로 근처 시장으로 발을 옮겨 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%광장시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%광장 시장%' LIMIT 1)
  ),
  NULL, 2,
  '광장시장에서 빈대떡·마약김밥 등 길거리 음식을 즐기며 북적이는 로컬 분위기를 체험해 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%세종마을%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%세종%' AND name ILIKE '%음식%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%세종문화회관%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%종로%' AND name ILIKE '%음식%' LIMIT 1)
  ),
  NULL, 3,
  '세종마을 음식거리에서 부담 없는 한식으로 마무리하면 현지인처럼 하루를 보낸 기분을 느낄 수 있어요.'
);

-- [남산·현대] 경리단길 → 이태원 일대 → N서울타워
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경리단%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%경리단길%' LIMIT 1)
  ),
  NULL, 1,
  '남산타워 야경 전, 경리단길 골목의 카페·바에서 로컬스럽게 힙한 분위기를 먼저 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%이태원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Itaewon%' LIMIT 1)
  ),
  NULL, 2,
  '이태원과 맞닿은 동네 골목을 걸으며 덜 관광지화된 거리의 색다른 매력을 찾아보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%N서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%서울타워%' LIMIT 1)
  ),
  NULL, 3,
  '마지막으로 남산에서 도시 야경을 감상하며 하루의 하이라이트를 올려 보세요.'
);

-- [홍대·힙] 홍대 거리 → 연남동 → 망원시장
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%홍익대학교%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대거리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대%' LIMIT 1)
  ),
  NULL, 1,
  '홍대 거리에서 버스킹과 거리 공연을 즐기며 에너지 넘치는 오후를 시작해 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연남동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연트럴파크%' LIMIT 1)
  ),
  NULL, 2,
  '연남동 골목의 로컬 카페(예: Coconutbox)에서 쉬어 가며 유명지와 다른 분위기를 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원 시장%' LIMIT 1)
  ),
  NULL, 3,
  '망원시장에서 떡볶이·호떡 등을 사 먹으며 시장의 활기와 산책으로 하루를 마무리해 보세요.'
);

-- ====================================================================
-- 유명·로컬 균형 코스 3종: 종로 혼합 / 홍대 로컬 / 남산 힐링
-- ====================================================================
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링'
));
DELETE FROM tour_courses WHERE title IN (
  '종로 혼합 코스', '궁궐과 시장, 하루의 균형: 종로 혼합 산책',
  '홍대 로컬 코스', '거리는 유명하게, 심장은 로컬로: 홍대 하루',
  '남산 힐링 코스', '골목 커피 후 불빛 위로: 남산 힐링'
);

INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES
('궁궐과 시장, 하루의 균형: 종로 혼합 산책', '#경복궁 #한복 #광장시장 #세종마을 #종로 #유명 #로컬 #혼합', 'https://tong.visitkorea.or.kr/cms/resource/21/2616021_image2_1.jpg', NOW()),
('거리는 유명하게, 심장은 로컬로: 홍대 하루', '#홍대 #연남동 #Coconutbox #망원시장 #버스킹 #로컬 #유명로컬반반', 'https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg', NOW()),
('골목 커피 후 불빛 위로: 남산 힐링', '#경리단길 #후암동 #로스터리 #N서울타워 #케이블카 #힐링 #야경', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW());

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Palace and Market in Balance: A Jongno Mixed Walk', '#Gyeongbokgung #Hanbok #GwangjangMarket #SejongVillage #Iconic #Local #Mix' FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Famous Streets, Local Heart: A Hongdae Day Split Half and Half', '#Hongdae #Yeonnamdong #Coconutbox #MangwonMarket #Busking #LocalVibes' FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Alley Coffee, Then Lights Above: Namsan Healing', '#Gyeongridan #Huamdong #Roastery #NSeoulTower #CableCar #Healing #NightView' FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '宮殿と市場のバランス：鐘路ミックス散策', '#景福宮 #韓服 #広蔵市場 #世宗村 #鐘路 #有名 #ローカル #ミックス' FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '道は有名に、心はローカルに：弘大の一日', '#弘大 #延南洞 #Coconutbox #望遠市場 #バスキング #ローカル' FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '路地のコーヒーのあと、光の上へ：南山ヒーリング', '#経理団路 #後岩洞 #ロースタリー #Nソウルタワー #ケーブルカー #癒し #夜景' FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '宫殿与市场平衡：钟路混合漫步', '#景福宫 #韩服 #广藏市场 #世宗村 #名胜 #本地 #混合' FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '街道要出名，内心偏本地：弘大一日', '#弘大 #延南洞 #望远市场 #路演 #本地氛围' FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '巷弄咖啡后，灯火之上：南山疗愈', '#理贤洞路 #厚岩洞 #烘焙 #N首尔塔 #缆车 #疗愈 #夜景' FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링'
ON CONFLICT (course_id, language) DO NOTHING;

-- [종로 혼합] 경복궁(유명) → 광장시장(로컬) → 세종마을 음식거리(로컬)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경복궁%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Gyeongbokgung%' LIMIT 1)
  ),
  NULL, 1,
  '유명 관광지인 경복궁에서 한복 체험으로 전통의 무게를 먼저 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%광장시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%광장 시장%' LIMIT 1)
  ),
  NULL, 2,
  '로컬의 심장인 광장시장으로 넘어가 빈대떡을 먹으며 시장의 활기를 온몸으로 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%세종마을%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%세종%' AND name ILIKE '%음식%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%세종문화회관%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%종로%' AND name ILIKE '%음식%' LIMIT 1)
  ),
  NULL, 3,
  '세종마을 음식거리에서 저렴한 한식으로 저녁을 마무리하면 전통 궁궐과 현지 시장이 자연스럽게 이어진 하루가 완성됩니다.'
);

-- [홍대 로컬] 홍대(유명) → 연남 Coconutbox(로컬) → 망원시장(로컬)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%홍익대학교%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대거리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대%' LIMIT 1)
  ),
  NULL, 1,
  '유명한 홍대 거리에서 버스킹과 쇼핑으로 에너지를 채운 뒤, 골목으로 발길을 옮겨 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연남동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연트럴파크%' LIMIT 1)
  ),
  NULL, 2,
  '연남동 로컬 카페(예: Coconutbox)에서 쉬며 젊은 힙스터 문화와 골목의 온도를 동시에 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원 시장%' LIMIT 1)
  ),
  NULL, 3,
  '망원시장에서 떡볶이·호떡을 먹으며 산책하면 일상 시장과 유명 거리가 50:50로 어우러진 매력을 느낄 수 있어요.'
);

-- [남산 힐링] 경리단길 → 후암동 로스터리 → N서울타워(케이블카)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경리단%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%경리단길%' LIMIT 1)
  ),
  NULL, 1,
  '케이블카를 타기 전 경리단길 골목 카페를 탐방하며 로컬스러운 힐링 시간을 가져 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%후암%' AND name ILIKE '%로스터%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%후암동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%용산%' AND name ILIKE '%로스터%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산%' AND name ILIKE '%카페%' LIMIT 1)
  ),
  NULL, 2,
  '후암동 로스터리에서 커피 한 잔과 함께 서울 전경을 감상하며 야경 명소 전 여유를 채워 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%N서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산서울타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%서울타워%' LIMIT 1)
  ),
  NULL, 3,
  'N서울타워에서 케이블카·전망으로 도시 야경을 마주하며 유명 랜드마크와 숨은 골목이 균형 잡힌 하루를 마무리해 보세요.'
);

-- ====================================================================
-- 로컬 중심 + 유명 30% 혼합 코스 3종: 종로 로컬 / 마포 골목 / 용산 힙플
-- ====================================================================
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
));
DELETE FROM tour_courses WHERE title IN (
  '종로 로컬 중심', '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심',
  '마포 골목 탐방', '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방',
  '용산 힙플 혼합', '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
);

INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES
('도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심', '#광장시장 #통인시장 #빈대떡 #떡볶이 #도시락 #경복궁 #한복 #종로 #로컬 #유명30', 'https://tong.visitkorea.or.kr/cms/resource/21/2616021_image2_1.jpg', NOW()),
('호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방', '#망원시장 #연남동 #Coconutbox #홍대 #버스킹 #호떡 #어묵 #마포 #로컬 #유명30', 'https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg', NOW()),
('브런치는 그레인, 산책은 남산: 용산 힙플 혼합', '#경리단길 #그레인서울 #남산둘레길 #브런치 #야경 #용산 #로컬 #유명30', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW());

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'One Lunchbox Stall, One Palace Shot: Jongno Local-First', '#GwangjangMarket #TonginMarket #Bindaetteok #Tteokbokki #Dosirak #Gyeongbokgung #Hanbok #LocalFirst #Iconic30' FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Hotteok Aroma, Hongdae in Passing: Mapo Alley Walk', '#MangwonMarket #Yeonnamdong #Coconutbox #Hongdae #Busking #LocalVibes #Iconic30' FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Brunch at Grain, Stroll on Namsan: Yongsan Hip Mix', '#Gyeongridan #GrainSeoul #NamsanTrail #Brunch #CityView #Romantic #Local #Iconic30' FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '弁当ひとマス、宮殿は一枚：鐘路ローカル中心', '#広蔵市場 #通仁市場 #ビンデトック #トッポッキ #ドシラク #景福宮 #韓服 #ローカル優先' FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', 'ホットクの香り、弘大はすれ違い：麻浦路地探訪', '#望遠市場 #延南洞 #Coconutbox #弘大 #バスキング #ローカル' FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', 'ブランチはグレイン、散策は南山：龙山ヒップミックス', '#経理団路 #GrainSeoul #南山トレイル #ブランチ #眺望 #ロマンティック' FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '便当一格，宫殿一帧：钟路本地优先', '#广藏市场 #通仁市场 #绿豆饼 #炒年糕 #便当 #景福宫 #韩服' FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '糖饼香气，擦肩弘大：麻浦巷弄探访', '#望远市场 #延南洞 #弘大 #路演 #本地' FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '早午餐在Grain，散步在南山：龙山潮流混合', '#理贤洞路 #Grain首尔 #南山步道 #早午餐 #城市景观' FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합'
ON CONFLICT (course_id, language) DO NOTHING;

-- [종로 로컬 중심] 광장시장(로컬) → 통인시장(로컬) → 경복궁(유명 30%)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%광장시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%광장 시장%' LIMIT 1)
  ),
  NULL, 1,
  '광장시장에서 빈대떡·떡볶이를 먹으며 로컬 시장의 활기를 먼저 채워 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%통인시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%통인 시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%통인%' AND name ILIKE '%시장%' LIMIT 1)
  ),
  NULL, 2,
  '통인시장에서 도시락 체험으로 한 칸 한 칸 골라 담는 재미를 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경복궁%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Gyeongbokgung%' LIMIT 1)
  ),
  NULL, 3,
  '경복궁에서는 전통 한복 스냅만 찍고 가볍게 빠져나오면 시장 활기와 궁궐이 자연스럽게 어우러진 하루가 됩니다.'
);

-- [마포 골목 탐방] 망원시장(로컬) → 연남 Coconutbox(로컬) → 홍대 거리(유명 30%)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원시장%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원 시장%' LIMIT 1)
  ),
  NULL, 1,
  '망원시장에서 호떡·어묵을 사 먹으며 골목과 시장을 산책해 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연남동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연트럴파크%' LIMIT 1)
  ),
  NULL, 2,
  '연남동 로컬 카페(예: Coconutbox)에서 커피 한 잔으로 힙한 동네 분위기를 즐겨 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%홍익대학교%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대거리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%홍대%' LIMIT 1)
  ),
  NULL, 3,
  '홍대 거리에서는 버스킹만 구경하며 스쳐 지나가도 현지 골목이 주인공인 코스가 완성됩니다.'
);

-- [용산 힙플 혼합] 경리단길(로컬) → 그레인서울(로컬) → 남산 둘레길(유명 30%)
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%경리단%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%경리단길%' LIMIT 1)
  ),
  NULL, 1,
  '경리단길 골목의 바·카페를 탐방하며 로컬 힙플 거리의 분위기를 느껴 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%그레인%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%Grain%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%용산%' AND name ILIKE '%브런치%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%이태원%' AND name ILIKE '%카페%' LIMIT 1)
  ),
  NULL, 2,
  '그레인서울에서 브런치로 하루의 무게를 올리며 로맨틱한 맛집 시간을 가져 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%남산%' AND name ILIKE '%둘레%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산%' AND name ILIKE '%산책%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%남산타워%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%N서울타워%' LIMIT 1)
  ),
  NULL, 3,
  '남산 둘레길을 짧게 걸으며 서울 전경을 감상하면 로컬 맛집과 가벼운 야경이 어우러진 하루를 마무리할 수 있어요.'
);

-- ====================================================================
-- 마포·서대문 로컬 산책 코스 3종: 연남 골목·공원 / 연희 라이프 / 망원 동네
-- ====================================================================
DELETE FROM user_saved_courses WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_course_items WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_course_translations WHERE course_id IN (SELECT id FROM tour_courses WHERE title IN (
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
));
DELETE FROM tour_courses WHERE title IN (
  '연남동 골목&공원 순례', '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례',
  '연희동 로컬 라이프 탐방', '성당 골목부터 천까지: 연희 로컬 라이프',
  '망원동 로컬 동네 투어', '자전거는 한강, 마음은 골목: 망원 로컬 투어'
);

INSERT INTO tour_courses (title, hashtags, featured_image, created_at)
VALUES
('아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례', '#연남동 #연트리공원 #연남누리길 #연남공원 #벽화거리 #책방 #LP바 #산책 #로컬', 'https://tong.visitkorea.or.kr/cms/resource/18/4012118_image2_1.jpg', NOW()),
('성당 골목부터 천까지: 연희 로컬 라이프', '#연희동 #연희동성당 #연희공원 #연희천 #서전서점 #벽화거리 #산책 #로컬', 'https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg', NOW()),
('자전거는 한강, 마음은 골목: 망원 로컬 투어', '#망원한강공원 #망원동 #망원문화마을 #포은로 #자전거 #일몰 #한강 #로컬', 'https://tong.visitkorea.or.kr/cms/resource/46/2800046_image2_1.jpg', NOW());

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Morning Nuri Trail, Night Murals: Yeonnam Alley & Park Pilgrimage', '#Yeonnamdong #YeontreePark #YeonnamNuriGil #YeonnamPark #MuralAlley #Bookstore #LPBar #Walk #Local' FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'From Cathedral Alleys to the Creek: Yeonhui Local Life', '#Yeonhui #YeonhuiCathedral #YeonhuiPark #Yeonhuicheon #Bookstore #MuralStreet #EveningWalk #Local' FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'ENG', 'Bike the Hangang, Heart in the Alleys: Mangwon Neighborhood Tour', '#MangwonHangangPark #Mangwon #CultureVillage #Poeunro #BikeRental #Sunset #HanRiver #Local' FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '朝はヌリコロ、夕は壁画：延南路地・公園巡礼', '#延南洞 #ヨントゥリ公園 #延南ヌリコロ #延南公園 #壁画路地 #本屋 #LPバー' FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '聖堂の路地から川まで：延禧ローカルライフ', '#延禧洞 #延禧洞聖堂 #延禧公園 #延禧川 #書店 #壁画通り' FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'JPN', '自転車は漢江、心は路地：望遠ローカルツアー', '#望遠漢江公園 #望遠洞 #文化村 #抱恩路 #自転車 #夕景' FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어'
ON CONFLICT (course_id, language) DO NOTHING;

INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '清晨走“新路”，傍晚看壁画：延南路巷与公园', '#延南洞 #延树公园 #延南新路 #延南公园 #壁画巷 #书店' FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '从教堂巷到小溪：延禧本地生活', '#延禧洞 #延禧教堂 #延禧公园 #延禧川 #书店 #壁画街' FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프'
ON CONFLICT (course_id, language) DO NOTHING;
INSERT INTO tour_course_translations (course_id, language, title, hashtags)
SELECT id, 'CHS', '车骑汉江，心在巷弄：望远本地邻里', '#望远汉江公园 #望远洞 #文化村 #抱恩路 #自行车 #日落' FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어'
ON CONFLICT (course_id, language) DO NOTHING;

-- [연남동 골목&공원 순례] 연트리·누리길 → 연남 골목(책방·LP) → 연남공원
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연트리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남누리%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연트럴파크%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' AND name ILIKE '%공원%' LIMIT 1)
  ),
  NULL, 1,
  '아침 9시쯤 연남누리길·연트리공원 산책로를 따라 느긋하게 걸으며 러닝족·강아지 산책하는 일상을 엿보세요. 평일 오전엔 한국인 비율이 높은 편이에요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연남동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' LIMIT 1)
  ),
  NULL, 2,
  '골목에서 벽화거리·고양이길 사진을 찍고, 점심 후엔 작은 서점·LP바는 창문 너머 구경만 하며 프리랜서들의 작업 분위기를 느껴 보세요. 편한 신발이 필수입니다.'
),
(
  (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연남공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연트럴파크%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연남%' AND name ILIKE '%공원%' LIMIT 1)
  ),
  NULL, 3,
  '오후엔 연남공원 벤치에 앉아 책을 읽거나 피크닉하는 커플·가족 옆에서 쉬고, 저녁 6시 무렵 가로등이 켜지면 골목 벽화 투어로 사진 명소 분위기를 즐겨 보세요.'
);

-- [연희동 로컬 라이프] 연희로·성당 → 연희공원 → 연희천
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연희동성당%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희%' AND name ILIKE '%성당%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희로%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희동%' LIMIT 1)
  ),
  NULL, 1,
  '브런치 후 11시쯤 연희역 근처에서 시작해 연희로 골목을 걸으며 연희동성당 주변 오래된 한옥·카페 외관만 가볍게 구경해 보세요. 사진 좋은 골목이 많으니 카메라를 챙기면 좋아요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연희공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희%' AND name ILIKE '%공원%' LIMIT 1)
  ),
  NULL, 2,
  '연희공원 순환로를 돌며 요가·산책하는 주민들의 일상 스팟을 느껴 보세요. 오후 2시쯤 서전서점·골목 LP바는 창문 구경만으로도 분위기가 살아납니다.'
),
(
  (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%연희천%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희%' AND name ILIKE '%천%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희로11%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%연희동%' LIMIT 1)
  ),
  NULL, 3,
  '연희로11가길 벽화거리에서 사진을 남긴 뒤, 저녁 7시쯤 연희천 산책로를 따라 야경과 함께 현지인들의 저녁 산책 코스를 걸어 보세요.'
);

-- [망원동 로컬 동네 투어] 망원한강공원 → 망원동 골목 → 문화마을·항동 일대
INSERT INTO tour_course_items (course_id, item_type, attraction_id, event_id, sequence_order, ai_comment)
VALUES
(
  (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원한강%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원%' AND name ILIKE '%한강공원%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%한강공원%' AND name ILIKE '%망원%' LIMIT 1)
  ),
  NULL, 1,
  '망원역 2번 출구 쪽에서 망원한강공원 자전거길을 타며 아침 10시 러닝·데이트족의 활기를 느껴 보세요. 공원 자전거 대여(약 1시간 3천 원대)로 이동도 효율적이에요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원%' AND name ILIKE '%마을%' LIMIT 1)
  ),
  NULL, 2,
  '11시쯤 포은로6길 일대 골목을 걸으며 예쁜 카페·벽화 동네를 느껴 보세요. 한국식 아파트·다가구 주택 일상은 항동 주택가 골목 산책에 녹여 보세요.'
),
(
  (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1),
  'ATTRACTION',
  COALESCE(
    (SELECT id FROM attraction WHERE name ILIKE '%망원문화마을%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원항동%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원%' AND name ILIKE '%문화%' LIMIT 1),
    (SELECT id FROM attraction WHERE name ILIKE '%망원한강%' LIMIT 1)
  ),
  NULL, 3,
  '망원문화마을길에서 작은 갤러리·DIY샵 외관을 구경한 뒤, 저녁 6시쯤 다시 한강공원 벤치로 돌아와 일몰과 가족 피크닉 분위기로 하루를 마무리해 보세요.'
);

-- --------------------------------------------------------------------
-- 코스 스텝 ai_comment 수동 번역 (한복·쇼핑·야경 묶음 이후 전 스텝, ENG/JPN/CHS)
-- CHT는 앱에서 CHS로 폴백되므로 생략 가능. 필요 시 동일 패턴으로 CHT 행 추가.
-- --------------------------------------------------------------------
-- [궁궐에서 골목까지: 한복 입은 하루]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Start your day at Gyeongbokgung in hanbok and feel the palace''s grandeur.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '景福宮で韓服を着て一日を始め、宮殿の荘厳さを体感しましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在景福宫穿韩服开启一天，感受宫殿的恢弘气势。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Stroll hanok alleys for a quiet mood and great photo spots.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '韓屋の路地を歩き、しっとりした情緒とフォトスポットを楽しみましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '漫步韩屋小巷，感受静谧氛围与拍照点。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Tea houses, souvenir lanes, and street snacks feel fresh even for first-time visitors.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '伝統茶房やお土産の路地、屋台スナックで外国人にも新鮮な魅力を味わえます。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '传统茶房、纪念品小巷与街头小吃，初次到访也会觉得新鲜有趣。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Ssamzie-gil''s layered alleys and quirky shops crown your heritage-themed day.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'サムジギルの立体的な路地と個性的な店で、伝統コースのフィナーレを飾りましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '森吉街立体巷弄与特色小店，为传统路线画上精彩句点。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐에서 골목까지: 한복 입은 하루' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [트렌드가 숨 쉬는 길: 홍대에서 코엑스까지]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Enjoy Hongdae busking and the street vibe, then hop between trendy cafés.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '弘大でバスキングと街の空気を楽しみ、トレンディなカフェ巡りへ。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在弘大享受街头表演与街区氛围，再串几家潮流咖啡馆。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Snap life shots at the trick-eye museum full of illusions and photo zones.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'だまし絵とフォトゾーン満載のトリックアートミュージアムで人生ショットを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在特丽爱美术馆的错视与拍照区留下人生美照。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At Gangnam Station''s underground and nearby malls, try VR, pop-ups, and youthful experiences.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '江南駅の地下街と周辺モールでVR・ポップアップなど若い感性の体験を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在江南站地下商街与周边商场体验VR、快闪等年轻玩法。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Blend shopping and a breather at COEX and the Starfield Library.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'COEXと星空図書館でトレンディなショッピングと休憩を一度に。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在COEX与星空图书馆兼顾潮流购物与歇脚放松。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '트렌드가 숨 쉬는 길: 홍대에서 코엑스까지' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [불빛 따라, 강까지: 서울의 밤 산책]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Ride the cable car or walk up Namsan for skyline views and a romantic night start.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'ケーブルカーか散策路で南山に上がり、都市の夜景でロマンチックな夜の始まりを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '乘缆车或步道登上南山，以都市夜景开启浪漫夜晚。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Walk Cheonggyecheon for night lights and an easy riverside pace.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '清渓川沿いを歩き、夜景とゆったりした川辺のペースを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '沿清溪川漫步，看夜景、享受河畔的悠闲步调。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'DDP''s futuristic architecture and lighting complete the urban night.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'DDPの未来的な建築とライトアップが都市の夜を仕上げます。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '东大门设计广场的前卫建筑与灯光，为城市夜晚点睛。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Finish with a river breeze and a stroll at a Hangang park.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '漢江公園で川風と散歩を楽しみ、一日をゆったり締めくくりましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在汉江公园吹江风散步，悠闲结束这一天。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '불빛 따라, 강까지: 서울의 밤 산책' LIMIT 1) AND sequence_order = 4 ON CONFLICT (course_item_id, language) DO NOTHING;

-- [궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Tour Gyeongbokgung for Joseon history, then head straight to the nearby market.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '景福宮で朝鮮の歴史を味わったら、すぐ近くの市場へ。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '先逛景福宫感受朝鲜历史，再移步附近市场。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At Gwangjang Market, try bindaetteok and mayak gimbap and soak up the busy local vibe.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '広蔵市場でビンデトックや麻薬キンパを楽しみ、にぎやかなローカル感を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在广藏市场吃绿豆饼、迷你紫菜包饭，感受热闹的本地氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Finish with easy Korean dishes on Sejong Village Food Street—eat like a local.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '世宗村グルメ通りで気軽な韓食に締めくくり、現地の一日を味わいましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在世宗村美食街用轻松的韩餐收尾，像本地人一样过完一天。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐 한 바퀴, 시장 한 그릇: 종로 역사와 맛' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [골목의 온도, 불빛 위로: 경리단과 남산]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Before Namsan night views, feel Gyeongridan''s hip cafés and bars in the alleys.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '南山の夜景の前に、経理団路の路地カフェ・バーでヒップな空気を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '登南山看夜景前，先在理贤洞路巷弄里的咖啡馆与酒吧感受潮酷氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Walk Itaewon-adjacent alleys for a less touristy, neighborhood feel.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '梨泰院に続く路地を歩き、観光地化しきっていない街の魅力を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '漫步靠近梨泰院的巷弄，感受不那么景区化的街区魅力。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'End on Namsan with the city lights as the day''s highlight.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '最後は南山で都市の光を眺め、一日のハイライトに。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '最后在南山欣赏都市灯火，把这一天推向高潮。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목의 온도, 불빛 위로: 경리단과 남산' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Kick off with busking and street energy on Hongdae''s main strip.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '弘大のメインストリートでバスキングと街の熱量で午後のスタートを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '从弘大主街的路演与街头活力开启下午。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Rest at a Yeonnam local café (e.g. Coconutbox)—a different vibe from the main sights.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '延南洞のローカルカフェ（例：Coconutbox）で一息、メイン観光地とは違う空気を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在延南洞本地咖啡馆（如 Coconutbox）歇脚，感受与主景区不同的氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Wrap up with tteokbokki and hotteok at Mangwon Market—market buzz plus a stroll.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '望遠市場でトッポッキやホットクを食べ歩き、市場の活気で締めくくり。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在望远市场吃炒年糕、糖饼边逛边收尾，把热闹与散步凑成一天。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리 위 리듬, 시장의 뜨거움: 홍대에서 망원까지' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;

-- [궁궐과 시장, 하루의 균형: 종로 혼합 산책]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Feel the weight of tradition with hanbok at iconic Gyeongbokgung first.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '有名な景福宮で韓服体験から、まず伝統の重みを感じましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '先在名胜景福宫穿韩服体验，感受传统的分量。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Move to Gwangjang—the local heart—and bite into bindaetteok amid market energy.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'ローカルの心臓・広蔵市場へ移り、ビンデトックで市場の熱気を全身で。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '转到本地心脏广藏市场，吃绿豆饼，把市场的热闹装进身体。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Close with affordable Korean dinner on Sejong Village Food Street—palace and markets in one day.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '世宗村グルメ通りでリーズナブルな韓食に締めくくれば、宮殿と市場が自然につながる一日に。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在世宗村美食街用平价韩餐收尾，宫殿与市集自然连成一天。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '궁궐과 시장, 하루의 균형: 종로 혼합 산책' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [거리는 유명하게, 심장은 로컬로: 홍대 하루]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Charge up on famous Hongdae with busking and shopping, then slip into the alleys.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '有名な弘大でバスキングとショッピングでエネルギーを補い、路地へ。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '先在出名的弘大路演、购物充满能量，再拐进巷弄。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Pause at a Yeonnam local café (e.g. Coconutbox) for hipster alley warmth.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '延南洞のローカルカフェ（例：Coconutbox）で休憩、ヒップな路地の温度を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在延南洞本地咖啡馆（如 Coconutbox）歇脚，感受潮人巷弄的温度。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At Mangwon Market, tteokbokki and hotteok on a stroll mix daily market with famous streets fifty-fifty.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '望遠市場でトッポッキやホットクを食べ歩き、日常の市場と有名ストリートが半々で調和する魅力を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在望远市场吃炒年糕、糖饼散步，日常市集与知名街区一半一半地交融。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '거리는 유명하게, 심장은 로컬로: 홍대 하루' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [골목 커피 후 불빛 위로: 남산 힐링]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Before the cable car, café-hop Gyeongridan alleys for a local, healing pause.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'ケーブルカーの前に、経理団路の路地カフェを巡りローカルな癒し時間を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '乘缆车前在理贤洞路巷弄咖啡馆巡礼，享受本地感疗愈时光。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At a Huam-dong roastery, sip coffee with Seoul views and calm before the night viewpoint.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '後岩洞のロースタリーでコーヒーとソウルの景色を味わい、夜景スポット前の余裕を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在后岩洞烘焙馆喝咖啡、看首尔全景，为夜景点前留出从容。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'From N Seoul Tower, cable car and views balance landmark glow with hidden alleys.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'Nソウルタワーでケーブルカーと眺望から都市の夜景へ、ランドマークと隠れた路地のバランス良い一日の締めを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在N首尔塔用缆车与展望面对都市夜景，地标光辉与隐秘巷弄平衡收尾。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '골목 커피 후 불빛 위로: 남산 힐링' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Start with bindaetteok and tteokbokki at Gwangjang Market for local buzz.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '広蔵市場でビンデトックやトッポッキを食べ、ローカル市場の活気から。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在广藏市场吃绿豆饼、炒年糕，先把本地市场的热闹装满。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At Tongin Market, pick lunchbox coins stall by stall—it''s half the fun.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '通仁市場の弁当コイン体験で、一皿ずつ選ぶ楽しさを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在通仁市场用便当币一格一格选菜，体验拼盘的乐趣。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'At Gyeongbokgung, a quick hanbok snap then slip out—market energy meets palace in one light day.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '景福宮では韓服スナップだけ軽く撮って抜ければ、市場の活気と宮殿が自然に繋がる一日に。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在景福宫只拍韩服快照就轻快走开，市集活力与宫殿自然连成轻松一天。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '도시락 한 칸, 궁궐은 한 컷: 종로 로컬 중심' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Snack on hotteok and fish cakes at Mangwon Market and stroll alleys with the crowd.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '望遠市場でホットクやおでんを食べ歩き、路地と市場を散策。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在望远市场买糖饼、鱼糕边走边吃，逛巷弄与市集。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Coffee at a Yeonnam local spot (e.g. Coconutbox) keeps the vibe hip but grounded.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '延南洞のローカルカフェ（例：Coconutbox）でコーヒー、ヒップな街の空気を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在延南洞本地咖啡馆来一杯咖啡，延续潮酷街区氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'On Hongdae street, even a quick busking pass keeps alleys as the real star.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '弘大ストリートはバスキングをちら見するだけでも、主役は路地のコースに。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '弘大街头哪怕只擦肩看看路演，主角仍是巷弄路线。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '호떡 향 따라, 홍대는 스쳐: 마포 골목 탐방' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [브런치는 그레인, 산책은 남산: 용산 힙플 혼합]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Bar- and café-hop Gyeongridan alleys for a local hip mix.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '経理団路の路地のバー・カフェを巡り、ローカルヒップの空気を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '逛理贤洞路巷弄里的酒吧与咖啡馆，感受本地潮流混合。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Brunch at Grain Seoul lifts the day with a romantic foodie pause.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'Grain Seoulでブランチを楽しみ、ロマンチックなグルメ時間を。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在Grain Seoul吃早午餐，把浪漫美食时光拉高。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'A short Namsan trail walk frames Seoul views—local bites plus soft night light.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '南山トレイルを短く歩きソウル全景を味わえば、ローカルグルメと軽い夜景が調和する締めに。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '短走南山环线看首尔全景，本地味道与轻柔夜景一起收尾。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '브런치는 그레인, 산책은 남산: 용산 힙플 혼합' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;

-- [아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Around 9 a.m., walk Yeonnam Nuri-gil and Yeontral Park—watch runners and dog walks; weekday mornings skew local. Comfortable shoes help.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '午前9時頃、延南ヌリコロとヨントゥリ公園の遊歩道をのんびり。ランナーや犬の散歩する日常が見え、平日午前は韓国人客が多め。歩きやすい靴推奨。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '上午九点左右沿延南“新路”与延树公园步道慢走，看跑步与遛狗日常；平日上午本地人比例高，建议穿舒适的鞋。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Shoot mural and “cat street” photos in the alleys; after lunch, peek bookstores and LP bars from outside to catch freelancers'' work vibe.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '路地で壁画・猫通りの写真を。昼食後は小さな書店やLPバーは外から覗くだけでもフリーランスの作業空気が伝わります。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在巷弄拍壁画与“猫咪街”；午饭后小书店、LP酒吧可从窗外感受自由工作者氛围。鞋一定要舒服。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Afternoon: rest on Yeonnam Park benches beside readers and picnickers; near 6 p.m., streetlights flip on—enjoy mural spots at blue hour.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '午後は延南公園のベンチで読書やピクニックする人々の横で一休み。夕方6時頃街灯が灯り、壁画散歩のブルーアワーを楽しみましょう。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '下午坐在延南公园长椅上看书、野餐的家庭与情侣旁休息；傍晚六点左右路灯亮起，适合壁画散步与拍照氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '아침은 누리길, 저녁은 벽화: 연남 골목·공원 순례' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [성당 골목부터 천까지: 연희 로컬 라이프]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'After brunch around 11, stroll Yeonhui-ro toward Yeonhui Catholic Church—glance old hanok and café façades; alleys are very photogenic.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', 'ブランチ後11時頃、延禧駅周辺から延禧路の路地を歩き、延禧洞聖堂付近の古い韓屋やカフェ外観を軽く。写真映え路地が多いのでカメラ推奨。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '早午餐后约十一点从延禧站附近出发，沿延禧路巷弄走向延禧洞天主堂，轻看老韩屋与咖啡馆立面；巷弄好拍，记得带相机。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Loop Yeonhui Park—yoga and walkers are the rhythm; around 2 p.m., Seojeon Bookstore and LP bar façades still tell the story from the window.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '延禧公園の周回路を回り、ヨガや散歩する住民のリズムを。午後2時頃、西田書店や路地のLPバーは窓越しでも空気が伝わります。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '绕延禧公园环线感受瑜伽与散步居民的日常；下午两点左右西田书店与巷弄LP酒吧光看橱窗也很有氛围。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Photos on Yeonhui-ro 11ga-gil murals, then about 7 p.m. walk Yeonhuicheon for night views with locals'' evening loop.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '延禧路11家路の壁画で写真を撮ったら、夕方7時頃は延禧川遊歩道で夜景と地元の夜散歩コースを。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '在延禧路11街壁画街拍照后，晚上七点左右沿延禧川步道看夜景，跟本地人一起散步收尾。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '성당 골목부터 천까지: 연희 로컬 라이프' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
-- [자전거는 한강, 마음은 골목: 망원 로컬 투어]
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'From Mangwon Station exit 2, ride the Hangang bike path—feel 10 a.m. runners and dates; rentals (~₩3k/hr) keep you efficient.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '望遠駅2番出口側から望遠漢江公園の自転車道へ。午前10時のランニングやデートの活気を。レンタル（約1時間3千ウォン台）も便利。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '从望远站2号口一侧上望远汉江公园自行车道，感受上午十点的跑步与约会活力；租车约一小时三千韩元档也省事。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 1 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Around 11, wander Poeun-ro 6-gil—cafés and murals—then peek Hangdong apartment alleys for everyday Seoul texture.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '11時頃は抱恩路6キル周辺の路地でカフェや壁画を。韓国式アパート・多世帯住宅の日常は항동住宅街路地散策に溶け込ませて。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '十一点左右逛抱恩路六巷一带的咖啡馆与壁画，再把韩国家庭式公寓区的日常叠进杏洞住宅巷散步。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 2 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'ENG', 'Browse small gallery and DIY shop façades on Mangwon Culture Village Road, then by ~6 p.m. return to Hangang benches for sunset picnics.' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'JPN', '望遠文化村通りで小さなギャラリーやDIY店の外観を見たら、夕方6時頃は再び漢江公園のベンチへ。日没と家族ピクニックの空気で締め。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
INSERT INTO tour_course_item_translations (course_item_id, language, ai_comment) SELECT id, 'CHS', '沿望远文化村路看小画廊与DIY店外观，傍晚六点再回到汉江公园长椅，用日落与家庭野餐氛围收尾。' FROM tour_course_items WHERE course_id = (SELECT id FROM tour_courses WHERE title = '자전거는 한강, 마음은 골목: 망원 로컬 투어' LIMIT 1) AND sequence_order = 3 ON CONFLICT (course_item_id, language) DO NOTHING;
