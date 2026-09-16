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

//收藏（愛心）
//api/me/** 由 AuthInterceptor 把關，能進來一定已登入；
@RestController
@RequiredArgsConstructor
public class CouponLikeController {

    private final CouponLikeService couponLikeService;

    @PostMapping("/api/me/coupons/{couponId}/like")
    public Map<String, Object> toggleLike(@PathVariable Integer couponId,
                                        @RequestAttribute("loginUser") Users loginUser) {
        return couponLikeService.toggleLike(loginUser.getId(), couponId);
    }

    public record BatchStatusRequest(List<Integer> couponIds) {}

    @PostMapping("/api/me/coupons/like/status-batch")
    public Map<Integer, Boolean> likeStatusBatch(
            @RequestBody BatchStatusRequest req,
            @RequestAttribute("loginUser") Users loginUser) {
        return couponLikeService.isLikedBatch(loginUser.getId(), req.couponIds());
    }
}