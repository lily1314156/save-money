package com.example.demo.service;

import com.example.demo.dao.BrandsDao;
import com.example.demo.dao.CouponsDao;
import com.example.demo.dao.StoresDao;
import com.example.demo.entity.Brands;
import com.example.demo.entity.Coupons;
import com.example.demo.entity.Stores;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CouponService {

    private final BrandsDao      brandsDao;
    private final StoresDao      storesDao;
    private final CouponsDao     couponsDao;

    // ── 品牌 ──────────────────────────────────────────
    public List<Brands> getAllBrands() {
        return brandsDao.selectAllBrands();
    }

    // ── 分店 ──────────────────────────────────────────
    public List<Stores> getAllStores() {
        return storesDao.selectAllStores();
    }

    public List<Coupons> getCouponsBrand(String brandSlug) {
        return couponsDao.selectAllBrandSlug(brandSlug);
    }

    public List<Coupons> getTodayCoupons(Integer userId) {
        return couponsDao.selectTodayCoupons(userId);
    }

    // 全部有效券（未過期），給 index「全部」tab 用
    public List<Coupons> getActiveCoupons() {
        return couponsDao.selectActiveCoupons();
    }

    // 向 DAO 請求 "有效" 的優惠券
    public List<Coupons> getCouponsByBrandIds(Collection<Integer> brandIds) {
        return couponsDao.selectActiveByBrandIds(brandIds);
    }

    // ── 我的券 ────────────────────────────────────────
    public List<Coupons> getMyCoupons(Integer userId, String category) {
        return couponsDao.selectMyCoupons(userId, category);
    }

    // 切換收藏（愛心）已搬到 CouponLikeService（含 Redis 快取同步）

    /**
     * 新增
     * 回傳：{ ok, id }
     */
    public Map<String, Object> createCoupon(Coupons coupon) {
        if (coupon.getCreatedAt() == null) {
            coupon.setCreatedAt(LocalDateTime.now());
        }
        if (coupon.getIsActive() == null) {
            coupon.setIsActive(true);
        }
        // affected > 0 就是判斷「有沒有改到」，轉成 ok: true / false 回給前端。
        int affected = couponsDao.insert(coupon);

        // 用 HashMap 而不是 Map.of，因為不允許 null value，
        // 萬一 insert 失敗 id 還是 null 就會炸
        Map<String, Object> result = new HashMap<>();
        result.put("ok", affected > 0);
        result.put("id", coupon.getId());
        return result;
    }

    public Map<String, Object> updateCoupon(Integer id, Coupons coupon) {
        coupon.setId(id);
        int affected = couponsDao.updateById(coupon);
        return Map.of("ok", affected > 0, "affected", affected);
    }

    /** 刪除一張券（硬刪） */
    public Map<String, Object> deleteCoupon(Integer id) {
        int affected = couponsDao.deleteById(id);
        return Map.of("ok", affected > 0, "affected", affected);
    }
}