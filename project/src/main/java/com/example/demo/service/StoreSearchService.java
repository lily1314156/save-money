package com.example.demo.service;

import com.example.demo.dao.BrandsDao;
import com.example.demo.dao.StoresDao;
import com.example.demo.entity.Brands;
import com.example.demo.entity.Stores;
import com.example.demo.service.GoogleMapService.PlaceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 給定一個座標，回 DB 裡距離排序的店家清單。
 * 距離計算用 Haversine 公式。
 */
@Service
@RequiredArgsConstructor
public class StoreSearchService {

    private final StoresDao storesDao;
    private final BrandsDao brandsDao;
    private final GoogleMapService googleMapService;
    // 地球半徑
    private static final double EARTH_RADIUS_KM = 6371.0;

    public List<StoreNearbyDto> findNearby(double lat, double lng, double radiusKm) {
        // 回傳附近的門市
        return storesDao.selectAllStores().stream()
                // 過濾掉 lat 或 lng 是 null
                .filter(s -> s.getLat() != null && s.getLng() != null)
                // 算每間門市跟你的距離
                .map(s -> StoreNearbyDto.from(s, haversineKm(lat, lng, s.getLat(), s.getLng())))
                // 超過半徑的丟掉
                .filter(dto -> dto.distanceKm() <= radiusKm)
                // 由近到遠排
                .sorted(Comparator.comparingDouble(StoreNearbyDto::distanceKm))
                .toList();
    }

    //先查 DB 沒有的品牌才打 Places API 補上去
    public List<StoreNearbyDto> findNearbyWithFallback(double lat, double lng, double radiusKm) {
        // 第一次查 DB
        List<StoreNearbyDto> firstPass = findNearby(lat, lng, radiusKm);

        // 已經在範圍內有的品牌 id
        Set<Integer> brandsWithStores = firstPass.stream()
                .map(StoreNearbyDto::brandId)
                .collect(Collectors.toSet());

        // 找出範圍內完全沒有的品牌
        List<Brands> missingBrands = brandsDao.selectAllBrands().stream()
                .filter(b -> !brandsWithStores.contains(b.getId()))
                .toList();

        if (missingBrands.isEmpty()) {
            // 都有了，不用打 Places 直接回
            return firstPass;
        }

        // 對每個缺的品牌打 Places API 把結果寫回 DB
        boolean addedAny = false;
        int radiusMeters = (int) (radiusKm * 500);

        for (Brands brand : missingBrands) {
            try {
                //拿這個 query 去打 textSearchBrand
                String query = pickSearchQuery(brand);
                List<PlaceResult> places = googleMapService.textSearchBrand(
                        lat, lng, query, radiusMeters, brand.getPlaceType());


                for (PlaceResult p : places) {
                    // name 過濾 Places 回的店名必須對得上品牌名或 slug
                    if (!isPlaceMatchingBrand(p, brand)) {
                        System.out.println("  ⊘ 過濾掉：" + p.name() + " (不像 " + brand.getName() + ")");
                        continue;
                    }

                    // 去相同品牌、同經緯度的店不重複 INSERT
                    // 處理 Google 回的同一家店、或同一店家被前一次搜尋已寫入的情況
                    if (storesDao.existsByBrandAndCoords(brand.getId(), p.lat(), p.lng())) {
                        System.out.println("  ⊘ 已存在：" + p.name() + " (同品牌同座標已有資料)");
                        continue;
                    }

                    // Text Search 不回電話/營業時間 → 用 place_id 多打一次 Place Details 補上
                    // 失敗就存 null（前端顯示「—」），不要讓整批搜尋炸掉
                    GoogleMapService.PlaceDetails details =
                            googleMapService.placeDetails(p.placeId()).orElse(null);

                    Stores newStore = Stores.builder()
                            .brandId(brand.getId())
                            .name(p.name())
                            .address(p.address())
                            .phone(details != null ? details.phone() : null)
                            .businessHours(details != null ? details.businessHours() : null)
                            .lat(p.lat())
                            .lng(p.lng())
                            .geocodedAt(LocalDateTime.now())
                            .isActive(true)
                            .build();
                    storesDao.insert(newStore);
                    addedAny = true;
                }
                // 休息 10ms
                Thread.sleep(10);
            } catch (Exception e) {
                // 單一品牌打失敗不要拖垮整批
                System.err.println("⚠ Places fallback 失敗：" + brand.getName() + " | " + e.getMessage());
            }
        }

        // 有 INSERT 才重查；沒有就直接回原本的
        return addedAny ? findNearby(lat, lng, radiusKm) : firstPass;
    }

    /**
     * 判斷 Places 撈到的店家是不是真的屬於這個品牌。
     * 比對順序： 品牌主名稱 → 品牌 slug → 別名aliases 
     * 任一命中就算過，全部對不上才擋。
     *
     * 範例：
     *   place="Starbucks星巴克 101典藏門市", brand.name="星巴克咖啡", slug="starbucks"
     *     → nameHit: contains("starbucks") in place? true → 過
     */
    private static boolean isPlaceMatchingBrand(PlaceResult place, Brands brand) {
        String placeNorm = normalize(place.name());

        // 主名稱 / slug / aliases 全部走同一套 keywordHit 規則
        if (keywordHit(placeNorm, normalize(brand.getName()))) return true;
        if (keywordHit(placeNorm, normalize(brand.getSlug()))) return true;

        String aliasesRaw = brand.getAliases();
        if (aliasesRaw != null && !aliasesRaw.isBlank()) {
            for (String alias : aliasesRaw.split(",")) {
                if (keywordHit(placeNorm, normalize(alias))) return true;
            }
        }
        return false;
    }

    /**
     * 關鍵字比對規則（方案一）：
     *   長度 >= 3 → contains 雙向比對
     *   長度 <  3 → 只允許「店名開頭」命中
     * 為什麼：短關鍵字用 contains 誤殺率極高，
     *   例：brand="OK" 會匹配到「行動卡拉ok設備」的器材行。
     *   改成 startsWith 後：「ok超商向上店」→ 過；「卡拉ok設備」→ 擋。
     */
    private static boolean keywordHit(String placeNorm, String keywordNorm) {
        if (keywordNorm.isEmpty()) return false;
        if (keywordNorm.length() < 3) {
            return placeNorm.startsWith(keywordNorm);
        }
        return placeNorm.contains(keywordNorm) || keywordNorm.contains(placeNorm);
    }

    /**
     * 挑搜尋字串（方案二）：
     *   主名稱長度 >= 3 → 直接用主名稱。
     *     消費者最常用的稱呼，Google 對它最準（例：50嵐、星巴克）。
     *   主名稱太短（"OK"、"全家"）→ 才從 aliases 挑最長的升級。
     *
     * 教訓：aliases 是給「比對」用的，不一定適合拿去「搜尋」。
     * 之前無條件挑最長，50嵐 會選到 "50lan"（5 字元 > 3 字元），
     * Google 用這個羅馬拼音根本搜不到店。
     */
    private static String pickSearchQuery(Brands brand) {
        String name = brand.getName();
        if (name != null && name.trim().length() >= 3) {
            return name;
        }

        String best = name;
        String aliasesRaw = brand.getAliases();
        if (aliasesRaw != null && !aliasesRaw.isBlank()) {
            for (String alias : aliasesRaw.split(",")) {
                String a = alias.trim();
                if (best == null || a.length() > best.length()) best = a;
            }
        }
        return best;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        // 去掉空白（半形、全形）+ 轉小寫
        return s.replace(" ", "").replace("　", "").toLowerCase();
    }

    // Haversine 公式：兩個經緯度點之間的球面距離（公里）
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sinHalfDLat = Math.sin(dLat / 2);
        double sinHalfDLng = Math.sin(dLng / 2);

        double a = sinHalfDLat * sinHalfDLat
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * sinHalfDLng * sinHalfDLng;

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // 用 record 寫，欄位是 immutable + 自動有 getter。
    public record StoreNearbyDto(
            Integer id,
            Integer brandId,
            String  name,
            String  address,
            String  phone,
            String  businessHours,
            Double  lat,
            Double  lng,
            double  distanceKm
    ) {
        // 從 Stores + 距離組出來，避免 controller 寫一長串 new
        public static StoreNearbyDto from(Stores s, double distanceKm) {
            return new StoreNearbyDto(
                    s.getId(),
                    s.getBrandId(),
                    s.getName(),
                    s.getAddress(),
                    s.getPhone(),
                    s.getBusinessHours(),
                    s.getLat(),
                    s.getLng(),
                    // 四捨五入到小數第 2 位，前端顯示「0.32 km」
                    Math.round(distanceKm * 100.0) / 100.0
            );
        }
    }
}