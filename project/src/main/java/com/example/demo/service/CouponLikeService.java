package com.example.demo.service;

import com.example.demo.dao.CouponsDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 收藏的 Redis 快取層。
 * 寫入順序永遠「先 DB、後 Redis」；Redis 掛了就直接刪 key，
 *
 * Key 設計（全部有 TTL，就算不一致也會自動修復）：
 *   uc:user:{userId}    Set    該使用者收藏中的 coupon_id   TTL 1hr ± 抖動
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
    private String userKey(Integer userId)    { return "uc:user:" + userId; }

    // ── 寫入 ──────────────────────────────────────────
    /**
     * 切換收藏。回傳 { ok, liked }，liked 是翻轉後的最新狀態。
     *
     *   1. DB toggle
     *   2. 從 DB 讀回最新狀態
     *   3. 同步 Redis：Set 加/減成員、計數器加/減一
     *      Redis 失敗 → 刪 key 強制下次重建，不影響回傳結果
     */
    public Map<String, Object> toggleLike(Integer userId, Integer couponId) {
        int affected = couponsDao.toggleLiked(userId, couponId);
        if (affected == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "收藏狀態更新失敗"
            );
        }

        Boolean liked = couponsDao.selectLiked(userId, couponId);
        boolean isLiked = Boolean.TRUE.equals(liked);

        syncCacheAfterToggle(userId, couponId, isLiked);

        return Map.of("ok", true, "liked", isLiked);
    }

    /** DB 寫完後同步快取；任何 Redis 例外都不往外丟（快取壞了靠 TTL 自癒，不能弄掛主流程） */
    private void syncCacheAfterToggle(
        Integer userId, Integer couponId, boolean isLiked) {
        String uKey = userKey(userId);
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
        } catch (Exception e) {
            log.warn("Redis 同步失敗，刪 key 讓下次重建。userId={}, couponId={}", userId, couponId, e);
            try {
                redis.delete(List.of(uKey));
            } catch (Exception ignored) {
                // Redis 整個掛了也沒關係，TTL 到期自然重建
            }
        }
    }

    
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

            Map<Object, Boolean> hits = redis.opsForSet().isMember(key, members);
            for (Integer id : couponIds) {
                result.put(id,
                        hits != null && Boolean.TRUE.equals(hits.get(String.valueOf(id))));
            }
        } catch (Exception e) {
            List<Integer> likedIds = couponsDao.selectLikedCouponIds(userId);
            for (Integer id : couponIds) {
                result.put(id, likedIds.contains(id));
            }
        }

        return result;
    }

    //cache miss 時從 DB 重建整個收藏 Set（含空收藏哨兵）
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