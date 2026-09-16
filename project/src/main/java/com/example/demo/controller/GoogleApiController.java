package com.example.demo.controller;

import com.example.demo.entity.Brands;
import com.example.demo.entity.Coupons;
import com.example.demo.entity.Stores;
import com.example.demo.entity.Users;
import com.example.demo.service.AuthService;
import com.example.demo.service.CouponService;
import com.example.demo.service.GoogleMapService;
import com.example.demo.service.GoogleMapService.GeoLocation;
import com.example.demo.service.NearbySearchService;
import com.example.demo.service.NearbySearchService.NearbySearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//REST API 端點集中地。
//Controller 只負責：收 HTTP 請求 → 呼叫 Service → 回傳 JSON。
@RestController
@RequiredArgsConstructor
public class GoogleApiController {

    private final CouponService couponService;
    private final GoogleMapService googleMapService;
    private final NearbySearchService nearbySearchService;
    private final AuthService authService;

    @GetMapping("/api/brands")
    public List<Brands> getBrands() {
        return couponService.getAllBrands();
    }

    @GetMapping("/api/stores")
    public List<Stores> getStores() {
        return couponService.getAllStores();
    }


    /**
     * 首頁
     * 自己讀 cookie：有登入 → 用真實 id（愛心會亮）；
     * 沒登入 → userId 傳 null，SQL 的 LEFT JOIN 匹配不到任何收藏，愛心全暗。
     */
    @GetMapping("/api/coupons/today")
    public List<Coupons> getTodayCoupons(
            @CookieValue(name = "token", required = false) String token) {
        Users loginUser = authService.getUserByToken(token);
        Integer userId = (loginUser != null) ? loginUser.getId() : null;
        return couponService.getTodayCoupons(userId);
    }

    //全部有效券且未過期
    @GetMapping("/api/coupons/all")
    public List<Coupons> getAllActiveCoupons() {
        return couponService.getActiveCoupons();
    }

    // 「我的券」
    //收藏且有效的券（強制登入）
    @GetMapping("/api/me/coupons")
    public List<Coupons> getMyCoupons(
            @RequestAttribute("loginUser") Users loginUser) {
        return couponService.getMyCoupons(loginUser.getId());
    }

    // POST /api/_admin/coupons 新增 id
    // 回傳：{"ok": true, "id": 42}
    @PostMapping("/api/_admin/coupons")
    public Map<String, Object> createCoupon(@RequestBody Coupons coupon) {
        return couponService.createCoupon(coupon);
    }

    //修改（有 id）
    @PutMapping("/api/_admin/coupons/{id}")
    public Map<String, Object> updateCoupon(@PathVariable Integer id,
                                            @RequestBody Coupons coupon) {
        return couponService.updateCoupon(id, coupon);
    }

    // 一次回 {center, stores, coupons}，前端打一支就夠
    //GET /api/nearby?lat=24.15&lng=120.65&radius=0.5
    @GetMapping("/api/nearby")
    public NearbySearchResult nearby(
            @RequestParam(name = "address",  required = false) String address,
            @RequestParam(name = "lat",      required = false) Double lat,
            @RequestParam(name = "lng",      required = false) Double lng,
            @RequestParam(name = "radius",   defaultValue = "0.5")     double radius,
            @RequestParam(name = "fallback", defaultValue = "false") boolean fallback) {

        // 優先用座標
        if (lat != null && lng != null) {
            return nearbySearchService.searchByCoordinates(lat, lng, null, radius, fallback);
        }
        // 沒座標才退回用地址
        if (address != null && !address.isBlank()) {
            return nearbySearchService.searchByAddress(address, radius, fallback);
        }
        throw new IllegalArgumentException("必須提供 address 或 lat+lng");
    }
}