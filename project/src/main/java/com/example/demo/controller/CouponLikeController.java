package com.example.demo.controller;

import com.example.demo.entity.Users;
import com.example.demo.service.CouponLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 收藏（愛心）相關端點集中地。
 * /api/me/** 由 AuthInterceptor 把關，能進來一定已登入；
 * like-count 是公開資料，不用登入。
 */
@RestController
@RequiredArgsConstructor
public class CouponLikeController {

    private final CouponLikeService couponLikeService;

    /**
     * 回傳 { ok: true, liked: true/false }，liked 是切換後的最新狀態，
     * 前端可以直接拿來決定愛心要亮還是暗，不用再猜。
     */
    @PostMapping("/api/me/coupons/{couponId}/like")
    public Map<String, Object> toggleLike(@PathVariable Integer couponId,
                                          @RequestAttribute("loginUser") Users loginUser) {
        return couponLikeService.toggleLike(loginUser.getId(), couponId);
    }

    /** 單張券是否已收藏（走 Redis）。回傳 { liked: true/false } */
    @GetMapping("/api/me/coupons/{couponId}/like/status")
    public Map<String, Object> likeStatus(@PathVariable Integer couponId,
                                          @RequestAttribute("loginUser") Users loginUser) {
        return Map.of("liked", couponLikeService.isLiked(loginUser.getId(), couponId));
    }

    /** 批次查詢請求的 body：{ "couponIds": [456, 789] } */
    public record BatchStatusRequest(List<Integer> couponIds) {}

    /**
     * 批次查收藏狀態，列表頁一次渲染整排愛心用。
     * 回傳 { "456": true, "789": false }
     */
    @PostMapping("/api/me/coupons/like/status-batch")
    public Map<Integer, Boolean> likeStatusBatch(@RequestBody BatchStatusRequest req,
                                                 @RequestAttribute("loginUser") Users loginUser) {
        return couponLikeService.isLikedBatch(loginUser.getId(), req.couponIds());
    }

    /** 某張券的收藏總數（公開，顯示熱度用）。回傳 { couponId, count } */
    @GetMapping("/api/coupons/{couponId}/like-count")
    public Map<String, Object> likeCount(@PathVariable Integer couponId) {
        return Map.of(
                "couponId", couponId,
                "count",    couponLikeService.getLikeCount(couponId)
        );
    }
}