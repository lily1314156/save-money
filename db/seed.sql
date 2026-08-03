USE save_money;

-- 清資料（只清 seed 負責的表；先清子表再清父表，避免 FK 卡住）
-- 注意：stores、brands、users 這裡【不能清】——
--   1. stores 是真資料，清了就沒了
--   2. DELETE FROM brands 會因 ON DELETE CASCADE 連鎖刪光所有 stores
--      （brands 改用下方的 upsert，id 永遠不變，stores.brand_id 才不會斷）
--   3. users 有真註冊帳號，改用第 4 段的 upsert，不清空
DELETE FROM user_coupons;
DELETE FROM coupons;

-- 重置自增 id（只重置有清空的表；users 沒清空，不能重置。
-- coupons 歸零後 id 又是 1~N，第 5 段寫死的 coupon_id 才對得上）
ALTER TABLE user_coupons AUTO_INCREMENT = 1;
ALTER TABLE coupons      AUTO_INCREMENT = 1;


-- ─────────────────────────────────────────────
-- 1) brands（14 個品牌）→ upsert
--    slug 是 UNIQUE，撞到同 slug 就走 UPDATE 更新內容，
--    沒撞到才 INSERT 新品牌。id 永遠不變。
--    slug 本身是比對錨點，所以不放進 UPDATE 清單。
-- ─────────────────────────────────────────────
-- place_type = Google Places 的類別，搜尋時給 Google 過濾用
--   超商/咖啡/速食的分類穩定 → 有設
--   手搖飲分類不穩定（cafe/store 都有人標）→ NULL 不過濾
INSERT INTO brands (name, slug, icon, sort_order, aliases, place_type) VALUES
  ('全家',     'familymart', '/brand photo/familymart.jpg', 10,  '全家便利商店,FamilyMart,family mart',    'convenience_store'),
  ('7-11',     '7eleven',    '/brand photo/7eleven.jpg',    20,  '統一超商,7-ELEVEN,711,Seven Eleven',     'convenience_store'),
  ('OK',       'ok',         '/brand photo/ok.png',         30,  'OK超商,OKmart,OK便利商店',               'convenience_store'),
  ('萊爾富',   'laerfu',     '/brand photo/laerfu.png',     40,  'Hi-Life,萊爾富便利商店',                 'convenience_store'),
  ('星巴克',   'starbucks',  '/brand photo/starbucks.jpg',  50,  'Starbucks,星巴克咖啡',                   'cafe'),
  ('麥當勞',   'mcdonald',   '/brand photo/mcdonald.png',   60,  'McDonald''s,mcdonalds',                  'restaurant'),
  ('肯德基',   'kfc',        '/brand photo/kfc.png',        70,  'KFC,Kentucky Fried Chicken',             'restaurant'),
  ('摩斯',     'mos',        '/brand photo/mos.jpg',        80,  'MOS Burger,摩斯漢堡',                    'restaurant'),
  ('路易莎',   'louisa',     '/brand photo/louisa.png',     90,  'Louisa Coffee,路易莎咖啡',               'cafe'),
  ('迷客夏',   'milksha',    '/brand photo/milksha.jpg',    100, 'Milksha,迷客夏綠光牧場主題飲品',         NULL),
  ('50嵐',     'fifty',      '/brand photo/fifty.jpg',      110, '五十嵐,50lan,50 嵐',                     NULL),
  ('Mr.Wish',  'mrwish',     '/brand photo/mrwish.png',     120, 'Mr.Wish鮮果茶飲,Mr Wish',                NULL),
  ('大苑子',   'dayuanzi',   '/brand photo/dayuanzi.png',   130, 'DaYungs,大苑子茶飲專賣店',               NULL),
  ('茶湯會',   'chatime',    '/brand photo/chatime.jpg',    140, 'TP TEA',                                 NULL)
AS new
ON DUPLICATE KEY UPDATE
  name       = new.name,
  icon       = new.icon,
  sort_order = new.sort_order,
  aliases    = new.aliases,
  place_type = new.place_type;
-- ↑ MySQL 8.0.19 以下不支援 AS new 寫法，若報語法錯誤，
--   改回舊寫法：name = VALUES(name), icon = VALUES(icon), ...


-- ─────────────────────────────────────────────
-- 2) stores：本檔不再插入
--    店家是真資料，由 StoreSearchService.findNearbyWithFallback()
--    打 Google Places API 自動補齊，seed 不碰。
-- ─────────────────────────────────────────────


-- ─────────────────────────────────────────────
-- 3) coupons（6 張券，原本 stores.html 裡的 50嵐 demo 資料）
-- ─────────────────────────────────────────────

SET @brand_fifty := (SELECT id FROM brands WHERE slug='fifty');

INSERT INTO coupons
  (brand_id,     type,        title,                                                start_date,   end_date,     discount,   price_tag, content,                 deal_text, terms) VALUES
-- (1) 50嵐 iPASS 優惠卷 $30
  (@brand_fifty, 'price',     '買 50嵐 就用\niPASS MONEY APP 付款',                  '2026-11-17', '2026-12-31', 30.00,      '優惠卷',  '累積消費滿99送',         NULL,
   JSON_ARRAY('每份面額為10元×3張，每人限得1份，活動總限量1.5萬份')),

-- (2) Uber Eats 胭脂買一送一
  (@brand_fifty, 'buy1get1',  'Uber Eats 胭脂系列限定優惠',                          '2026-05-12', '2026-08-25', NULL,       NULL,  '胭脂紅茶、胭脂冷露、胭脂歐蕾',                     '買一送一',
   JSON_ARRAY('優惠組數有限，贈完為止，依各門市現有組數為主。', '每帳號每日限使用優惠一次，每優惠品項最多買三送三。')),

-- (3) 50嵐 優惠卷 $20（demo：已被某人用掉）
  (@brand_fifty, 'price',     '已使用 50嵐 優惠券',                                  '2025-10-01', '2025-10-31', 20.00,      '優惠卷',  '消費滿99使用',           NULL,
   JSON_ARRAY('已核銷使用')),

-- (4) 冬季限定買一送一（demo：已被某人用掉）
  (@brand_fifty, 'buy1get1',  '冬季限定 買一送一',                                   '2025-01-01', '2025-01-31', NULL,       NULL,  '烏龍奶茶、珍珠奶茶',                     '買一送一',
   JSON_ARRAY('此優惠已使用完畢。')),

-- (5) 50嵐 折扣券 $10（已過期）
  (@brand_fifty, 'price',     '過期 50嵐 折扣券',                                    '2024-12-01', '2024-12-31', 10.00,      '折扣卷',  '滿49折10元',             NULL,
   JSON_ARRAY('優惠已過期。')),

-- (6) 夏日限定買一送一（已過期）
  (@brand_fifty, 'buy1get1',  '夏日限定 買一送一',                                   '2024-08-01', '2024-08-31', NULL,       NULL,  '仙草奶茶、四季春茶',                     '買一送一',
   JSON_ARRAY('優惠已過期。'));


SET @b_familymart := (SELECT id FROM brands WHERE slug='familymart');
SET @b_7eleven    := (SELECT id FROM brands WHERE slug='7eleven');
SET @b_ok         := (SELECT id FROM brands WHERE slug='ok');
SET @b_laerfu     := (SELECT id FROM brands WHERE slug='laerfu');
SET @b_starbucks  := (SELECT id FROM brands WHERE slug='starbucks');
SET @b_mcdonald   := (SELECT id FROM brands WHERE slug='mcdonald');
SET @b_kfc        := (SELECT id FROM brands WHERE slug='kfc');
SET @b_mos        := (SELECT id FROM brands WHERE slug='mos');
SET @b_louisa     := (SELECT id FROM brands WHERE slug='louisa');
SET @b_milksha    := (SELECT id FROM brands WHERE slug='milksha');
SET @b_mrwish     := (SELECT id FROM brands WHERE slug='mrwish');
SET @b_dayuanzi   := (SELECT id FROM brands WHERE slug='dayuanzi');
SET @b_chatime    := (SELECT id FROM brands WHERE slug='chatime');


INSERT INTO coupons
  (brand_id, type, title, start_date, end_date, discount, price_tag, content, deal_text, terms) VALUES
-- ── 全家 ──
(@b_familymart, 'price',    '全家 $30 折抵券',         '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY('單筆消費滿99元可使用')),
(@b_familymart, 'buy1get1', '全家 經典御飯糰 買一送一',  '2026-01-01', '2026-12-31', NULL,  NULL, '經典御飯糰系列',         '買一送一',  JSON_ARRAY('限同口味使用')),
(@b_familymart, 'price',    '全家 $50 折扣券',         '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY('單筆消費滿199元可使用')),
(@b_familymart, 'price',    '全家 $10 經典券',         '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 7-11 ──
(@b_7eleven,    'price',    '7-ELEVEN $30 折抵券',     '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY('可在門市與 OPEN POINT APP 使用')),
(@b_7eleven,    'buy1get1', '7-ELEVEN City Café 買一送一', '2026-01-01', '2026-12-31', NULL,  NULL, 'City Café 中杯拿鐵',         '買一送一', JSON_ARRAY('僅限現煮咖啡')),
(@b_7eleven,    'price',    '7-ELEVEN $50 折扣券',     '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_7eleven,    'price',    '7-ELEVEN $10 經典券',     '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── OK ──
(@b_ok,         'price',    'OK $30 折抵券',           '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_ok,         'buy1get1', 'OK 經典飯糰 買一送一',     '2026-01-01', '2026-12-31', NULL,  NULL, '經典飯糰系列',         '買一送一',    JSON_ARRAY('限現場兌換')),
(@b_ok,         'price',    'OK $50 折扣券',           '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_ok,         'price',    'OK $10 經典券',           '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 萊爾富 ──
(@b_laerfu,     'price',    '萊爾富 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_laerfu,     'buy1get1', '萊爾富 萊咖啡 買一送一',    '2026-01-01', '2026-12-31', NULL,  NULL, '萊咖啡中杯拿鐵',         '買一送一',  JSON_ARRAY()),
(@b_laerfu,     'price',    '萊爾富 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_laerfu,     'price',    '萊爾富 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 星巴克 ──
(@b_starbucks,  'price',    '星巴克 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY('需出示星禮程序會員碼')),
(@b_starbucks,  'buy1get1', '星巴克 那堤系列 買一送一',   '2026-01-01', '2026-12-31', NULL,  NULL, '那堤系列大杯',         '買一送一',    JSON_ARRAY('限大杯')),
(@b_starbucks,  'price',    '星巴克 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_starbucks,  'price',    '星巴克 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 麥當勞 ──
(@b_mcdonald,   'price',    '麥當勞 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY('可於麥當勞 APP 兌換')),
(@b_mcdonald,   'buy1get1', '麥當勞 大薯 買一送一',      '2026-01-01', '2026-12-31', NULL,  NULL, '大薯',         '買一送一',          JSON_ARRAY('限同筆訂單')),
(@b_mcdonald,   'price',    '麥當勞 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_mcdonald,   'price',    '麥當勞 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 肯德基 ──
(@b_kfc,        'price',    '肯德基 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_kfc,        'buy1get1', '肯德基 經典蛋撻 買一送一',   '2026-01-01', '2026-12-31', NULL,  NULL, '經典蛋撻',         '買一送一',       JSON_ARRAY('每筆訂單限用一次')),
(@b_kfc,        'price',    '肯德基 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_kfc,        'price',    '肯德基 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 摩斯 ──
(@b_mos,        'price',    '摩斯 $30 折抵券',          '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_mos,        'buy1get1', '摩斯 米漢堡 買一送一',      '2026-01-01', '2026-12-31', NULL,  NULL, '米漢堡系列',         '買一送一',     JSON_ARRAY()),
(@b_mos,        'price',    '摩斯 $50 折扣券',          '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_mos,        'price',    '摩斯 $10 經典券',          '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 路易莎 ──
(@b_louisa,     'price',    '路易莎 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_louisa,     'buy1get1', '路易莎 拿鐵 買一送一',      '2026-01-01', '2026-12-31', NULL,  NULL, '拿鐵咖啡',         '買一送一',      JSON_ARRAY('限大杯')),
(@b_louisa,     'price',    '路易莎 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_louisa,     'price',    '路易莎 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 迷客夏 ──
(@b_milksha,    'price',    '迷客夏 $30 折抵券',        '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_milksha,    'buy1get1', '迷客夏 鮮奶茶 買一送一',     '2026-01-01', '2026-12-31', NULL,  NULL, '招牌鮮奶茶系列',         '買一送一', JSON_ARRAY()),
(@b_milksha,    'price',    '迷客夏 $50 折扣券',        '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_milksha,    'price',    '迷客夏 $10 經典券',        '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 50嵐 ──
(@brand_fifty,  'price',    '50嵐 $30 折抵券',          '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@brand_fifty,  'buy1get1', '50嵐 紅茶拿鐵 買一送一',    '2026-01-01', '2026-12-31', NULL,  NULL, '紅茶拿鐵系列',         '買一送一',   JSON_ARRAY()),
(@brand_fifty,  'price',    '50嵐 $50 折扣券',          '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@brand_fifty,  'price',    '50嵐 $10 經典券',          '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── Mr.Wish ──
(@b_mrwish,     'price',    'Mr.Wish $30 折抵券',       '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_mrwish,     'buy1get1', 'Mr.Wish 鮮果茶 買一送一',   '2026-01-01', '2026-12-31', NULL,  NULL, '鮮果茶系列',         '買一送一',     JSON_ARRAY()),
(@b_mrwish,     'price',    'Mr.Wish $50 折扣券',       '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_mrwish,     'price',    'Mr.Wish $10 經典券',       '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 大苑子 ──
(@b_dayuanzi,   'price',    '大苑子 $30 折抵券',         '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_dayuanzi,   'buy1get1', '大苑子 鮮榨柳橙汁 買一送一', '2026-01-01', '2026-12-31', NULL,  NULL, '鮮榨柳橙汁',         '買一送一',     JSON_ARRAY('限當日新鮮製作')),
(@b_dayuanzi,   'price',    '大苑子 $50 折扣券',         '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_dayuanzi,   'price',    '大苑子 $10 經典券',         '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期')),

-- ── 茶湯會 ──
(@b_chatime,    'price',    '茶湯會 $30 折抵券',         '2026-01-01', '2026-12-31', 30.00, '優惠卷', '消費滿99送',  NULL,           JSON_ARRAY()),
(@b_chatime,    'buy1get1', '茶湯會 觀音拿鐵 買一送一',   '2026-01-01', '2026-12-31', NULL,  NULL, '觀音拿鐵',         '買一送一',       JSON_ARRAY()),
(@b_chatime,    'price',    '茶湯會 $50 折扣券',         '2026-01-01', '2026-12-31', 50.00, '折扣卷', '消費滿199減', NULL,           JSON_ARRAY()),
(@b_chatime,    'price',    '茶湯會 $10 經典券',         '2024-06-01', '2024-12-31', 10.00, '折扣卷', '消費滿49減',  NULL,           JSON_ARRAY('此優惠已過期'));


-- ─────────────────────────────────────────────
-- 6) 每日限定券：7-11、星巴克、摩斯、路易莎 各 5 張
-- ─────────────────────────────────────────────
INSERT INTO coupons
  (brand_id, type, title, start_date, end_date, discount, price_tag, content, deal_text, terms) VALUES

-- ── 7-11 ──
(@b_7eleven, 'price',    '7-ELEVEN 思樂冰 $20 折抵券',        '2026-05-02', '2026-05-06', 20.00, '優惠卷', '消費滿59折20',  NULL,                   JSON_ARRAY('每人限用一次')),
(@b_7eleven, 'buy1get1', '7-ELEVEN City Café 冰拿鐵 買一送一', '2026-05-02', '2026-05-06', NULL,  NULL, 'City Café 冰拿鐵中杯',            '買一送一',  JSON_ARRAY('每帳號限一次')),
(@b_7eleven, 'price',    '7-ELEVEN 御飯糰 $15 折抵券',        '2026-05-02', '2026-05-06', 15.00, '折扣卷', '消費滿45折15',  NULL,                   JSON_ARRAY()),
(@b_7eleven, 'buy1get1', '7-ELEVEN 霜淇淋 買一送一',          '2026-05-02', '2026-05-06', NULL,  NULL, '原味/巧克力霜淇淋',            '買一送一',      JSON_ARRAY('贈完為止')),
(@b_7eleven, 'price',    '7-ELEVEN 關東煮 $25 折抵券',        '2026-05-02', '2026-05-06', 25.00, '優惠卷', '消費滿99折25',  NULL,                   JSON_ARRAY()),

-- ── 星巴克 ──
(@b_starbucks, 'price',    '星巴克 冷萃咖啡 $50 折抵券', '2026-05-02', '2026-05-06', 50.00, '折扣卷', '消費滿199折50', NULL,            JSON_ARRAY('需出示星禮程序會員碼')),
(@b_starbucks, 'buy1get1', '星巴克 抹茶拿鐵 買一送一',   '2026-05-02', '2026-05-06', NULL,  NULL, '抹茶拿鐵大杯',            '買一送一',   JSON_ARRAY('限大杯')),
(@b_starbucks, 'price',    '星巴克 季節限定 $30 折抵券', '2026-05-02', '2026-05-06', 30.00, '優惠卷', '消費滿149折30', NULL,            JSON_ARRAY('僅限季節限定飲品')),
(@b_starbucks, 'buy1get1', '星巴克 冰搖茶 買一送一',     '2026-05-02', '2026-05-06', NULL,  NULL, '冰搖茶系列大杯',            '買一送一', JSON_ARRAY('限大杯')),
(@b_starbucks, 'price',    '星巴克 星冰樂 $40 折抵券',   '2026-05-02', '2026-05-06', 40.00, '折扣卷', '消費滿169折40', NULL,            JSON_ARRAY('限星冰樂系列')),

-- ── 摩斯 ──
(@b_mos, 'price',    '摩斯 珍珠奶茶米漢堡 $30 折抵券', '2026-05-02', '2026-05-06', 30.00, '優惠卷', '消費滿149折30', NULL,           JSON_ARRAY('限套餐使用')),
(@b_mos, 'buy1get1', '摩斯 薯條 買一送一',             '2026-05-02', '2026-05-06', NULL,  NULL, '摩斯薯條（大）',            '買一送一', JSON_ARRAY('限大份')),
(@b_mos, 'price',    '摩斯 辣味米漢堡 $20 折抵券',     '2026-05-02', '2026-05-06', 20.00, '折扣卷', '消費滿99折20',  NULL,           JSON_ARRAY('限辣味系列')),
(@b_mos, 'buy1get1', '摩斯 鮮乳茶 買一送一',           '2026-05-02', '2026-05-06', NULL,  NULL, '摩斯鮮乳茶系列',            '買一送一', JSON_ARRAY('限鮮乳茶系列')),
(@b_mos, 'price',    '摩斯 牛肉米漢堡 $25 折抵券',     '2026-05-02', '2026-05-06', 25.00, '優惠卷', '消費滿119折25', NULL,           JSON_ARRAY('限牛肉米漢堡系列')),

-- ── 路易莎 ──
(@b_louisa, 'price',    '路易莎 濃縮咖啡 $20 折抵券', '2026-05-02', '2026-05-06', 20.00, '優惠卷', '消費滿79折20',  NULL,           JSON_ARRAY()),
(@b_louisa, 'buy1get1', '路易莎 冰美式 買一送一',     '2026-05-02', '2026-05-06', NULL,  NULL, '冰美式（大）',            '買一送一',   JSON_ARRAY('限大杯')),
(@b_louisa, 'price',    '路易莎 貝果 $15 折抵券',     '2026-05-02', '2026-05-06', 15.00, '折扣卷', '消費滿59折15',  NULL,           JSON_ARRAY('限貝果商品')),
(@b_louisa, 'buy1get1', '路易莎 卡布奇諾 買一送一',   '2026-05-02', '2026-05-06', NULL,  NULL, '卡布奇諾（大）',            '買一送一', JSON_ARRAY('限大杯')),
(@b_louisa, 'price',    '路易莎 手作吐司 $25 折抵券', '2026-05-02', '2026-05-06', 25.00, '優惠卷', '消費滿99折25',  NULL,           JSON_ARRAY('限手作吐司系列'));
