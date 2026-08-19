@../AGENTS.md
- 工作方式
編輯前先看清楚檔案現在的完整程式碼，再開始動作。
單一檔案局部修改直接動手；但新功能或跨檔改動我還是會先確認方向。
功能拆成最小單位，一次只做一件事。整條刪除算「一件事」，先報範圍再執行。

- 溝通風格與行為邊界
解釋概念時給足脈絡，但不重複、不鋪陳。
運用 Judgment 配合上下文情境，自動拿捏程式碼提供的份量與解釋深度，避免死板的否定句限制。

- 專案：SAVE MONEY
- 專案說明
SAVE MONEY 是一個本地優惠券整合平台：消費者可以用地圖和定位找附近店家的優惠券，收藏自己想用的券。

- 專案根目錄
D:\discount coupon\

- 技術棧與專案慣例
後端：Spring Boot
模板引擎：Thymeleaf
ORM：MyBatis-Plus
資料庫：MySQL（Navicat 管理）/ Redis(dock)
前端：HTML / CSS（SCSS）/ JavaScript
啟動專案：VS Code

- 注意事項：此處僅保留非顯而易見的專案慣例與架構決策，其餘常規技術細節請直接從程式碼庫中推斷。

- 目錄結構
採用漸進式揭露原則 discount coupon/ ├── .claude/ │ └── .CLAUDE.md ├── seed.sql └── project/ (核心原始碼目錄，請依需求自行展開檢視)

- 資料流架構 flow：詳見 @flow/index.md
- 資料庫：詳見 @db/main-coupon.md