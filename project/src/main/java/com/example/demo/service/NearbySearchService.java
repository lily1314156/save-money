package com.example.demo.service;

import com.example.demo.entity.Coupons;
import com.example.demo.service.GoogleMapService.GeoLocation;
import com.example.demo.service.StoreSearchService.StoreNearbyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NearbySearchService {

    private final GoogleMapService    googleMapService;
    private final StoreSearchService  storeSearchService;
    private final CouponService       couponService;

     /* 
       fallback = true DB 缺 brands 會去打 Places API 補（並 cache 進 DB）
       return {center, stores, coupons}
     */
    public NearbySearchResult searchByAddress(String address, double radiusKm, boolean fallback) {
        // ── 步驟 1：地址 → 座標 ──
        GeoLocation loc = googleMapService.geocode(address)
                .orElseThrow(() -> new IllegalArgumentException("找不到地址：" + address));

        // 已知座標，共用 searchByCoordinates
        return searchByCoordinates(loc.lat(), loc.lng(), address, radiusKm, fallback);
    }


        // 用取得 navigator.geolocation 的座標
    public NearbySearchResult searchByCoordinates(double lat, double lng,
                                                  String addressForDisplay,     //給前端的地址；沒地址時可傳 null
                                                  double radiusKm, boolean fallback) {  //true 時 DB 缺資料的品牌會去打 Places API 補
        // ── 步驟 2：附近店家 ──
        //條件 ? 成立時的結果 : 不成立時的結果
        List<StoreNearbyDto> stores = fallback
                ? storeSearchService.findNearbyWithFallback(lat, lng, radiusKm)
                : storeSearchService.findNearby(lat, lng, radiusKm);

        // ── 步驟 3：抓出範圍內出現過哪些品牌（Set 去重）──
        Set<Integer> brandIds = stores.stream()
                .map(StoreNearbyDto::brandId)
                .collect(Collectors.toSet());

        // ── 步驟 4：對應的所有有效優惠券 ──
        List<Coupons> coupons = couponService.getCouponsByBrandIds(brandIds);

        // ── 步驟 5：包成回傳 ──
        // 沒地址字串就用「目前位置」當顯示文字，前端 console log 才不會印出 undefined
        String displayAddr = (addressForDisplay == null || addressForDisplay.isBlank())
                ? "目前位置"
                : addressForDisplay;
        return new NearbySearchResult(
                new Center(displayAddr, lat, lng),
                stores,
                coupons
        );
    }

    // ───────────── 回傳資料結構 ─────────────

    /**
     * 整合 endpoint 回給前端的 JSON 結構。
     * 前端能用：
     *   data.center.lat / lng / address  → 把地圖移到這
     *   data.stores                      → 跑迴圈插 marker 跟列清單
     *   data.coupons                     → 跑迴圈渲染優惠券區
     */
    public record NearbySearchResult(
            Center center,
            List<StoreNearbyDto> stores,
            List<Coupons> coupons
    ) {}

    /** 搜尋中心點：使用者輸入的地址 + Geocoding 算出的座標 */
    public record Center(String address, double lat, double lng) {}
}