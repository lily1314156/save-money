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
/*
 * REST API 端點集中地。
 * Controller 只負責：收 HTTP 請求 → 呼叫 Service → 回傳 JSON。
 */
@RestController
@RequiredArgsConstructor
public class GoogleApiController {

    private final CouponService couponService;
    private final GoogleMapService googleMapService;
    private final NearbySearchService nearbySearchService;
    private final AuthService authService;

    /** ──────────────────────────────────────────────
    [1] 使用者打開 index.html
        ↓
    [2] fetch('/api/brands')
            ↓
    [3] 收到請求，路由到 ApiController.getBrands()
            ↓
    [4] Controller 呼叫 couponService.getAllBrands()
            ↓
    [5] Service 呼叫 brandsDao.selectAllBrands()
            ↓
    [6] DAO 用 MyBatis 去 MySQL 撈 brands 表的所有資料
            ↓
    [7] 一路把 List<Brands> 往上回傳，Spring 自動轉成 JSON
            ↓
    [8] 前端 JS 拿到 JSON，渲染成畫面上
    ──────────────────────────────────────────────*/

    @GetMapping("/api/brands")
    public List<Brands> getBrands() {
        return couponService.getAllBrands();
    }

    @GetMapping("/api/stores")
    public List<Stores> getStores() {
        return couponService.getAllStores();
    }

    @GetMapping("/api/coupons")

    //@RequestParam("brand") 代表從 ?brand=xxx 取值
    //api/coupons?brand=fifty
    public List<Coupons> getCouponsByBrand(@RequestParam("brand") String brandSlug) {
        return couponService.getCouponsBrand(brandSlug);
    }

    /**
     * 首頁列表：訪客也能看，所以「可選登入」——
     * 不走攔截器，自己讀 cookie：有登入 → 用真實 id（愛心會亮）；
     * 沒登入 → userId 傳 null，SQL 的 LEFT JOIN 匹配不到任何收藏，愛心全暗。
     */
    @GetMapping("/api/coupons/today")
    public List<Coupons> getTodayCoupons(
            @CookieValue(name = "token", required = false) String token) {
        Users loginUser = authService.getUserByToken(token);
        Integer userId = (loginUser != null) ? loginUser.getId() : null;
        return couponService.getTodayCoupons(userId);
    }

    /** 全部有效券（is_active=1 且未過期），index「全部」tab 用 */
    @GetMapping("/api/coupons/all")
    public List<Coupons> getAllActiveCoupons() {
        return couponService.getActiveCoupons();
    }


    // ──────────────────────────────────────────────
    // 「我的券」
    // ──────────────────────────────────────────────

    /** GET /api/me/coupons?category=latest（強制登入，由 AuthInterceptor 把關） */
    @GetMapping("/api/me/coupons")
    public List<Coupons> getMyCoupons(
            @RequestParam(name = "category", defaultValue = "latest") String category,
            @RequestAttribute("loginUser") Users loginUser) {
        return couponService.getMyCoupons(loginUser.getId(), category);
    }


    /**
     * GET /api/geocode?address=台中市政府
     *
     * 回傳：
     *   找到 → {"found": true, "address": "...", "lat": 24.16, "lng": 120.65}
     *   找不到 → {"found": false, "address": "..."}
     *   key 壞、IP 不對 → 500 + 例外訊息
     */
    @GetMapping("/api/geocode")
    public Map<String, Object> geocode(@RequestParam("address") String address) {
        Optional<GeoLocation> result = googleMapService.geocode(address);

        if (result.isPresent()) {
            GeoLocation loc = result.get();
            return Map.of(
                    "found",   true,
                    "address", address,
                    "lat",     loc.lat(),
                    "lng",     loc.lng()
            );
        } else {
            return Map.of(
                    "found",   false,
                    "address", address
            );
        }
    }

    // ──────────────────────────────────────────────
    // Admin：券的增刪改
    //   POST   /coupons       新增（沒 id）
    //   PUT    /coupons/{id}  修改（有 id）
    //   DELETE /coupons/{id}  刪除
    // ──────────────────────────────────────────────

    /*
     * POST /api/_admin/coupons
     * {
     *   "brandId": 1,
     *   "type": "price",
     *   "title": "夏季優惠",
     * }
     * 回傳：{"ok": true, "id": 42}
     */
    @PostMapping("/api/_admin/coupons")
    public Map<String, Object> createCoupon(@RequestBody Coupons coupon) {
        return couponService.createCoupon(coupon);
    }

    @PutMapping("/api/_admin/coupons/{id}")
    public Map<String, Object> updateCoupon(@PathVariable Integer id,
                                            @RequestBody Coupons coupon) {
        return couponService.updateCoupon(id, coupon);
    }

    /*
     * id 存在{"ok" : true, "affected": 1}
     * 不存在{"ok" : false, "affected": 0}
     */
    @DeleteMapping("/api/_admin/coupons/{id}")
    public Map<String, Object> deleteCoupon(@PathVariable Integer id) {
        return couponService.deleteCoupon(id);
    }

    // ──────────────────────────────────────────────
    // 整合 endpoint
    // 一次回 {center, stores, coupons}，前端打一支就夠
    // ──────────────────────────────────────────────

    //GET /api/nearby?lat=24.15&lng=120.65&radius=1

    @GetMapping("/api/nearby")
    public NearbySearchResult nearby(
            @RequestParam(name = "address",  required = false) String address,
            @RequestParam(name = "lat",      required = false) Double lat,
            @RequestParam(name = "lng",      required = false) Double lng,
            @RequestParam(name = "radius",   defaultValue = "1")     double radius,
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