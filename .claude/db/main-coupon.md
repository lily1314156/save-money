## 資料庫 main-coupon

### brands（品牌）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | INT, PK, AUTO_INCREMENT | 主鍵 |
| name | VARCHAR(50) | 顯示名稱，例：50嵐 |
| slug | VARCHAR(50), UNIQUE | JS 用的 id，例：fifty |
| bg_color | VARCHAR(20) | 品牌背景色 |
| icon | VARCHAR(255) | 圖片路徑或連結 |
| sort_order | INT, DEFAULT 0 | 排序，數字小優先 |
| is_active | TINYINT(1), DEFAULT 1 | 1=啟用 / 0=停用 |
| aliases | VARCHAR(255) | 別名（搜尋用） |
| place_type | VARCHAR(50) NULL COMMENT | Google Places type 過濾類別 |

### users（使用者）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | INT, PK, AUTO_INCREMENT | 主鍵 |
| email | VARCHAR(120), UNIQUE | 登入帳號 |
| password_hash | VARCHAR(255) | 加密後密碼 |
| nickname | VARCHAR(50) | 顯示暱稱 |
| created_at | DATETIME, DEFAULT NOW() | 註冊時間 |

### stores（分店）→ FK: brands.id
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | INT, PK, AUTO_INCREMENT | 主鍵 |
| brand_id | INT, FK | 所屬品牌 |
| name | VARCHAR(100) | 分店名稱 |
| address | VARCHAR(255) | 地址 |
| business_hours | VARCHAR(100) | 營業時間 |
| phone | VARCHAR(30) | 電話 |
| is_active | TINYINT(1), DEFAULT 1 | 1=啟用 / 0=停用 |
| lat | DECIMAL(10,8) | 緯度 |
| lng | DECIMAL(11,8) | 經度 |
| geocoded_at | DATETIME | 最後地理編碼時間 |

### coupons（優惠券）→ FK: brands.id
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | INT, PK, AUTO_INCREMENT | 主鍵 |
| brand_id | INT, FK | 所屬品牌 |
| type | VARCHAR(20) | 'price' 或 'buy1get1' |
| title | VARCHAR(255) | 優惠券標題 |
| start_date | DATE | 開始日期 |
| end_date | DATE | 結束日期（過期判斷用） |
| discount | DECIMAL(10,2) | 折扣金額，例：30.00 |
| price_tag | VARCHAR(20) | 標籤文字，例：折扣卷 |
| content | VARCHAR(255) | 說明文字 |
| deal_text | VARCHAR(50) | 買一送一說明文字 |
| terms | JSON | 展示用條款（純顯示） |
| is_active | TINYINT(1), DEFAULT 1 | 1=啟用 / 0=停用 |
| created_at | DATETIME, DEFAULT NOW() | 建立時間 |

### user_coupons（使用者持有券）→ FK: users.id, coupons.id
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 主鍵 |
| user_id | INT, FK | 所屬使用者 |
| coupon_id | INT, FK | 對應優惠券 |
| liked | TINYINT(1), DEFAULT 0 | 1=已收藏 |
| claimed_at | DATETIME, DEFAULT NOW() | 領取時間 |

### 依賴關係
brands ──┬──< stores
└──< coupons ──< user_coupons >── users