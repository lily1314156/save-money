# 收藏（愛心）流程

## 1. 使用者點愛心 → index.html / stores.html `toggleHeart(btn)`
1. `pendingLikes` Set 擋連點：同一張券還在飛就直接 return
2. `wasLiked = btn.classList.contains('liked')` 存回滾用快照
3. `btn.classList.toggle('liked', !wasLiked)` 樂觀更新（先變色，不等後端）
4. ↓ fetch POST `/api/me/coupons/{id}/like`
   - 沒有 body，couponId 在路徑上；Cookie 的 token 由瀏覽器自動帶
   - 這裡不用 `fetchJson`：要自己分辨 401 和其他錯誤，fetchJson 一律丟 Error

## 2. AuthInterceptor.preHandle() —— 進 Controller 前先被攔
1. WebConfig 只攔 `/profile` 和 `/api/me/**`，這支剛好落在 `/api/me/**`
2. Cookie 取出 `token` → `authService.getUserByToken(token)` → Redis `token:{uuid}` 反序列化出 Users
3. 查無 → 因為 URI 以 `/api/` 開頭，回 401（不轉址），`return false`，Controller 完全不執行
4. 通過 → `request.setAttribute("loginUser", user)`

## 3. CouponLikeController.toggleLike(couponId, loginUser)
1. 參數
   - `@PathVariable Integer couponId`：從網址取 id
   - `@RequestAttribute("loginUser") Users loginUser`：直接拿 interceptor 掛上去的 user，不用再查一次 DB
2. 沒有 dto、沒有 `@Valid`：參數只有一個路徑上的整數。型別轉不出來（`/api/me/coupons/abc/like`）Spring 自己丟 MethodArgumentTypeMismatchException → 400
3. 呼叫 `couponLikeService.toggleLike(loginUser.getId(), couponId)`

## 4. CouponLikeService.toggleLike(userId, couponId)
1. `couponsDao.toggleLiked()` → `CouponsMapper.toggleLiked()`（@Insert）
   - `INSERT INTO user_coupons (user_id, coupon_id, liked) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE liked = 1 - liked`
   - 第一次收藏 → insert 一筆 liked=1；之後 → 撞上 (user_id, coupon_id) 唯一鍵走 UPDATE 把 liked 翻面
   - 「取消收藏」不是 DELETE，是把 liked 設成 0，紀錄留著
   - 回傳 affected rows
2. `affected == 0` → `throw new ResponseStatusException(HttpStatus.CONFLICT, "收藏狀態更新失敗")`
   - 註冊是回 200 + `{failed:true}`；這裡改成丟例外，讓 HTTP 狀態碼本身代表失敗
   - 實務上進不去：liked 是 NOT NULL、`1 - liked` 必定改值，且 Connector/J 預設 `useAffectedRows=false`
   - 券不存在會先被外鍵 `fk_uc_coupon` 擋成 DataIntegrityViolationException（目前無 @ControllerAdvice → 500，尚未處理）
3. `couponsDao.selectLiked()` 讀回翻轉後的權威狀態
   - `SELECT liked FROM user_coupons WHERE user_id=? AND coupon_id=?`，回 Boolean（沒紀錄是 null）
   - `Boolean.TRUE.equals(liked)` 轉 primitive，順便把 null 當成 false
   - 為什麼不自己在記憶體算：DB 才是真的，同一個帳號別的分頁可能剛改過
4. `syncCacheAfterToggle(userId, couponId, isLiked)` 同步 Redis
   - key `uc:user:{userId}`（Set，存這個人收藏中的 coupon_id），TTL 1hr + 最多 10% 抖動（防雪崩）
   - 順序永遠「先 DB、後 Redis」
   - 只有 `hasKey` 為 true 才做增量：收藏 → `sadd` 並移掉 `__empty__` 哨兵；取消 → `srem`
   - key 不存在就什麼都不做，留給下次讀取整批重建
   - 整段包 try/catch：Redis 例外只記 log + `delete(key)` 強制下次重建，絕不往外丟（快取壞了不能弄掛主流程）
5. `return Map.of("ok", true, "liked", isLiked)`

## 5. 回到前端
1. 401 → 回滾成 wasLiked，confirm 問要不要去 `/login`
2. 其他非 2xx（409）→ 回滾 + console.error
3. 成功 → `btn.classList.toggle('liked', liked)`（第二參數是「設定」不是「切換」）→ `updateCouponCache(id, liked)` 寫回 `couponsCache.all / latest`
4. `finally` → `pendingLikes.delete(id)`

## 6. 回傳格式
- 成功：200 `{ ok: true, liked: true/false }` — liked 是切換後的權威狀態
- 失敗：409，body 是 Spring 預設錯誤格式，不是 `{ ok:false }`；前端只看 `res.ok`，不讀失敗 body
- 要讓 message 出現在 body 需設 `server.error.include-message=always`（目前沒設）

## 7. 同 Controller 第二支：POST /api/me/coupons/like/status-batch
1. `@RequestBody BatchStatusRequest req`（record，`List<Integer> couponIds`）→ Jackson 反序列化
2. `couponLikeService.isLikedBatch(userId, couponIds)`
   - 空 list → 直接回空 Map
   - `hasKey` false → `rebuildUserSet()`：`SELECT coupon_id FROM user_coupons WHERE user_id=? AND liked=1` 整批寫進 Set；沒收藏就放 `__empty__` 哨兵（防快取穿透），再 `expire` TTL
   - `opsForSet().isMember(key, members)`（SMISMEMBER）一次問多個成員
   - try/catch：Redis 掛了 fallback 走 DB `selectLikedCouponIds` + contains 比對
3. 回 `Map<Integer, Boolean>` → `{ "12": true, "15": false }`
4. 用途：`/api/coupons/all` 刻意不 JOIN liked（保持使用者無關、可快取），清單拿到後再補查愛心；today API 有 JOIN 自帶 liked 不用補

## 8. 跟註冊流程的差別
| | 註冊 | 收藏 |
|---|---|---|
| 登入 | 不用 | 一定要（AuthInterceptor） |
| 參數 | dto RegisterRequest + @Valid | PathVariable + RequestAttribute，無 dto |
| 失敗 | 200 + `{failed:true, message}` | 丟例外 → 409，用狀態碼表達 |
| 快取 | 無 | Redis Set + 哨兵 + TTL 抖動 |
| 冪等 | 否 | 是 toggle，同一支 API 收藏／取消都走它 |

## 9. 已知問題
- `stores.html:281` 呼叫 `applyLikedStatus(...)`，但全專案沒有這個函式的定義 → ReferenceError，且在 try 裡會被 catch 吃掉，變成「⚠ 無法載入優惠券（applyLikedStatus is not defined）」，「全部」分頁整個渲染不出來。status-batch 目前等於沒有呼叫者。
- index 走「全部」來源時沒有補查 liked，愛心初始全暗。
- `syncCacheAfterToggle` 的 hasKey → add/remove 之間有 TOCTOU：key 剛好在中間過期，會 sadd 出一個沒有 TTL、內容殘缺的 key，永不自癒。補 `expire` 或改 Lua。
- 401 導登入頁沒帶 redirect-back。
