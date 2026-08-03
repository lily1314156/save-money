-- ============================================================
-- save_money 資料庫結構（單一檔 schema）
--
-- 排序原則（不關閉 FOREIGN_KEY_CHECKS）：
--   Teardown（DROP）：子 → 父
--   Setup  （CREATE）：父 → 子
-- 被參照的表永遠先存在、後刪除，所以 FK 檢查全程開著，
-- 一旦順序寫錯會立刻報錯（這是好事，幫你抓 bug）。
--
-- 依賴關係：
--   brands ─┬─< stores
--           └─< coupons ─┐
--   users ──────────────┴─< user_coupons
--
-- 重跑安全：開頭 DROP 子→父，可重複執行。
-- 種子資料另放 seed.sql，這支只負責建結構。
-- ============================================================

CREATE DATABASE IF NOT EXISTS save_money
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE save_money;


-- ============================================================
-- Teardown：子 → 父（先拆子表，最後拆父表）
-- ============================================================
DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS stores;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS brands;


-- ============================================================
-- Setup：父 → 子（先建父表，子表的 FK 才有對象可指）
-- ============================================================

-- ── 父表 1：brands（品牌）─────────────────────────────
CREATE TABLE brands (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,                 -- 顯示用品牌名（例：50嵐）
    slug        VARCHAR(50)  NOT NULL UNIQUE,          -- 對應原本 JS 的 id（例：fifty）
    bg_color    VARCHAR(20)  NULL,
    icon        VARCHAR(255) NULL,                     -- 圖片路徑或連結
    sort_order  INT          NOT NULL DEFAULT 0,       -- 排序用，數字小優先
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,       -- 1 啟用 / 0 停用（TINYINT 模擬 Boolean）
    aliases     VARCHAR(255) NULL,
    place_type  VARCHAR(50)  NULL
) CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 子表 1：stores（分店）→ 依賴 brands ───────────────
-- 一個品牌可以有多家分店；地址、營業時間、電話放這裡。
CREATE TABLE stores (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    brand_id        INT          NOT NULL,
    name            VARCHAR(100) NOT NULL,             
    address         VARCHAR(255) NULL,
    business_hours  VARCHAR(100) NULL,
    phone           VARCHAR(30)  NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    lat             DECIMAL(10, 8) NULL,
    lng             DECIMAL(11, 8) NULL,
    geocoded_at     DATETIME NULL,

    CONSTRAINT fk_stores_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE,

    INDEX idx_stores_brand (brand_id)
) CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 子表 2：coupons（折價券主表，單表多型）→ 依賴 brands ─
-- type=price    用 face_value、price_tag
-- type=buy1get1 用 deal_text、deal_sub
-- terms 是 JSON，存展示用文字（期限規則等），不查 SQL。
CREATE TABLE coupons (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    brand_id            INT          NOT NULL,
    type                VARCHAR(20)  NOT NULL,          -- 'price' | 'buy1get1'
    title               VARCHAR(255) NOT NULL,

    -- 期限：拉成獨立欄位，方便 SQL 過濾「未過期」
    start_date          DATE         NULL,
    end_date            DATE         NULL,

    -- 價格型專用
    discount          DECIMAL(10,2) NULL,             -- $30 → 30.00
    price_tag           VARCHAR(20)   NULL,             -- 優惠卷 / 折扣卷
    content     VARCHAR(255)  NULL,             -- 「累積消費滿99送」

    -- 買一送一型專用
    deal_text           VARCHAR(50)   NULL,             -- 「買一送一」

    -- 顯示用條款（多行文字陣列，純展示）
    terms               JSON         NULL,

    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_coupons_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE,

    CONSTRAINT chk_coupons_type
        CHECK (type IN ('price', 'buy1get1')),

    INDEX idx_coupons_brand_end (brand_id, end_date),
    INDEX idx_coupons_type (type)
) CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ── 父表 2：users（使用者）────────────────────────────
CREATE TABLE users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- ── 子表 3：user_coupons（使用者持有的券）→ 依賴 users、coupons ─
-- 靠 coupons.end_date < NOW() 即時判斷。
-- liked（收藏）也放這裡。
CREATE TABLE user_coupons (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    coupon_id   INT          NOT NULL,
    liked       TINYINT(1)   NOT NULL DEFAULT 0,
    claimed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_uc_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uc_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,

    -- 一個人對同一張券只能有一筆紀錄
    UNIQUE KEY uq_user_coupon (user_id, coupon_id)

) CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
