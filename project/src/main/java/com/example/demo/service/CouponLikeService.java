package com.example.demo.service;

import com.example.demo.dao.CouponsDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 收藏的 Redis 快取層。
 *
 * DB 是唯一真相（Source of Truth），Redis 只是加速讀取。
 * 寫入順序永遠「先 DB、後 Redis」；Redis 掛了就直接刪 key，
 * 讓下次讀取時從 DB 重建，比嘗試修補簡單可靠。
 *
 * Key 設計（全部有 TTL，就算不一致也會自動修復）：
 *   uc:user:{userId}    Set    該使用者收藏中的 coupon_id   TTL 1hr ± 抖動
 *   uc:count:{couponId} String 該券的收藏總數                TTL 10min ± 抖動
 *
 * 為什麼用 StringRedisTemplate 而不是專案原本的 RedisTemplate<String,Object>：
 * SISMEMBER / INCR 要求 Redis 裡存純字串，
 * 原本的 JSON 序列化會把 "456" 存成 "\"456\""，成員比對會永遠 false。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponLikeService {

    private final CouponsDao couponsDao;
    private final StringRedisTemplate redis;

    /** 空收藏哨兵值：沒有任何收藏也要放一個成員，才能區分「快取不存在」和「收藏是空的」，防止快取穿透 */
    private static final String EMPTY_SENTINEL = "__empty__";

    private static final Duration TTL_USER_SET = Duration.ofHours(1);
    private static final Duration TTL_COUNT    = Duration.ofMinutes(10);

    private String userKey(Integer userId)    { return "uc:user:" + userId; }
    private String countKey(Integer couponId) { return "uc:count:" + couponId; }

    // ── 寫入 ──────────────────────────────────────────

    /**
     * 切換收藏。回傳 { ok, liked }，liked 是翻轉後的最新狀態。
     *
     * 流程（Write-Through，先 DB 後 Redis）：
     *   1. DB toggle（ON DUPLICATE KEY UPDATE，冪等）
     *   2. 從 DB 讀回最新狀態 —— 以 DB 為準，不自己猜
     *   3. 同步 Redis：Set 加/減成員、計數器加/減一
     *      Redis 失敗 → 刪 key 強制下次重建，不影響回傳結果
     */
    public Map<String, Object> toggleLike(Integer userId, Integer couponId) {
        int affected = couponsDao.toggleLiked(userId, couponId);
        if (affected == 0) {
            return Map.of("ok", false);
        }

        Boolean liked = couponsDao.selectLiked(userId, couponId);
        boolean isLiked = Boolean.TRUE.equals(liked);

        syncCacheAfterToggle(userId, couponId, isLiked);

        return Map.of("ok", true, "liked", isLiked);
    }

    /** DB 寫完後同步快取；任何 Redis 例外都不往外丟（快取壞了靠 TTL 自癒，不能弄掛主流程） */
    private void syncCacheAfterToggle(Integer userId, Integer couponId, boolean isLiked) {
        String uKey = userKey(userId);
        String cKey = countKey(couponId);
        try {
            // 只在 key 已存在時做增量更新；key 不存在就留給下次讀取時整批重建，
            // 避免對不存在的 key INCR 造出一個錯誤的計數（INCR 對不存在的 key 會從 0 開始）
            if (Boolean.TRUE.equals(redis.hasKey(uKey))) {
                if (isLiked) {
                    redis.opsForSet().add(uKey, String.valueOf(couponId));
                    redis.opsForSet().remove(uKey, EMPTY_SENTINEL);
                } else {
                    redis.opsForSet().remove(uKey, String.valueOf(couponId));
                }
            }
            if (Boolean.TRUE.equals(redis.hasKey(cKey))) {
                if (isLiked) redis.opsForValue().increment(cKey);
                else         redis.opsForValue().decrement(cKey);
            }
        } catch (Exception e) {
            log.warn("Redis 同步失敗，刪 key 讓下次重建。userId={}, couponId={}", userId, couponId, e);
            try {
                redis.delete(List.of(uKey, cKey));
            } catch (Exception ignored) {
                // Redis 整個掛了也沒關係，TTL 到期自然重建
            }
        }
    }

    // ── 讀取 ──────────────────────────────────────────

    /**
     * 單張券是否已收藏（高頻端點，走 Redis）。
     *   1. key 存在 → SISMEMBER，O(1)
     *   2. key 不存在（cache miss）→ 從 DB 撈全部收藏回填 Set，再判斷
     *   3. Redis 掛了 → 直接查 DB，功能不中斷
     */
    public boolean isLiked(Integer userId, Integer couponId) {
        try {
            String key = userKey(userId);
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                rebuildUserSet(userId);
            }
            return Boolean.TRUE.equals(
                    redis.opsForSet().isMember(key, String.valueOf(couponId)));
        } catch (Exception e) {
            log.warn("Redis 讀取失敗，退回 DB。userId={}", userId, e);
            return Boolean.TRUE.equals(couponsDao.selectLiked(userId, couponId));
        }
    }

    /**
     * 批次查收藏狀態（列表頁一次渲染整排愛心用）。
     * 用 SMISMEMBER（isMember 多值版）一次查完，避免 N 次 Redis 往返。
     * 回傳 { couponId: true/false, ... }
     */
    public Map<Integer, Boolean> isLikedBatch(Integer userId, List<Integer> couponIds) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        if (couponIds == null || couponIds.isEmpty()) return result;

        try {
            String key = userKey(userId);
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                rebuildUserSet(userId);
            }
            Object[] members = couponIds.stream()
                                        .map(String::valueOf)
                                        .toArray();
            // SMISMEMBER：一個指令回傳每個成員在不在 Set 裡
            Map<Object, Boolean> hits = redis.opsForSet().isMember(key, members);
            for (Integer id : couponIds) {
                result.put(id, hits != null && Boolean.TRUE.equals(hits.get(String.valueOf(id))));
            }
        } catch (Exception e) {
            log.warn("Redis 批次讀取失敗，退回 DB。userId={}", userId, e);
            List<Integer> likedIds = couponsDao.selectLikedCouponIds(userId);
            for (Integer id : couponIds) {
                result.put(id, likedIds.contains(id));
            }
        }
        return result;
    }

    /**
     * 某張券的收藏總數。
     * 先 GET uc:count:{id}，miss 才 COUNT(*) 回填。
     * 計數器短 TTL（10 分鐘）→ 就算 INCR/DECR 漂移，也會定期從 DB 重算校正。
     */
    public long getLikeCount(Integer couponId) {
        String key = countKey(couponId);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
            long count = couponsDao.countLikes(couponId);
            redis.opsForValue().set(key, String.valueOf(count), withJitter(TTL_COUNT));
            return count;
        } catch (Exception e) {
            log.warn("Redis 計數失敗，退回 DB。couponId={}", couponId, e);
            return couponsDao.countLikes(couponId);
        }
    }

    // ── 內部工具 ──────────────────────────────────────

    /** cache miss 時從 DB 重建整個收藏 Set（含空收藏哨兵） */
    private void rebuildUserSet(Integer userId) {
        List<Integer> likedIds = couponsDao.selectLikedCouponIds(userId);

        String[] members = likedIds.isEmpty()
                ? new String[]{ EMPTY_SENTINEL }
                : likedIds.stream().map(String::valueOf).toArray(String[]::new);

        String key = userKey(userId);
        redis.opsForSet().add(key, members);
        redis.expire(key, withJitter(TTL_USER_SET));
    }

    /** TTL 加隨機抖動（最多 +10%），避免大量 key 同時過期造成快取雪崩 */
    private Duration withJitter(Duration base) {
        long jitter = ThreadLocalRandom.current().nextLong(base.getSeconds() / 10 + 1);
        return base.plusSeconds(jitter);
    }
}
