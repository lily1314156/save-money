package com.example.demo.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Coupons;
import com.example.demo.mapper.CouponsMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public class CouponsDao {

    @Autowired
    private CouponsMapper couponsMapper;

    /** 取得指定品牌（slug）所有啟用且未過期的券 */
    public List<Coupons> selectAllBrandSlug(String slug) {
        return couponsMapper.selectAllBrandSlug(slug);
    }

    /** 用 id 取得單一張券 */
    public Coupons selectId(Integer id) {
        return couponsMapper.selectById(id);
    }

    /**
     * 當日限定券：CURDATE() 介於 start_date 與 end_date 之間且 is_active=1。
     * LEFT JOIN user_coupons 把該使用者的 liked 狀態帶出來，前端愛心要用。
     */
    public List<Coupons> selectTodayCoupons(Integer userId) {
        return couponsMapper.selectTodayCoupons(userId);
    }

    /**
     * 取得使用者持有的券，依 category 分類：
     *   'latest'  → c.end_date >= 今天
     *   'expired' → c.end_date <  今天
     */
    public List<Coupons> selectMyCoupons(Integer userId, String category) {
        return couponsMapper.selectMyCoupons(userId, category);
    }

    /**
     * 取得「全部」有效券：is_active=1 且 end_date >= 今天。
     * 給 index.html 的「全部」tab 用。
     * 注意：這裡用 MyBatis-Plus 的 selectList，沒有 join user_coupons，
     *      所以 liked 欄位會是 null（前端愛心預設不亮）。
     */
    public List<Coupons> selectActiveCoupons() {
        LambdaQueryWrapper<Coupons> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupons::getIsActive, true)
               .ge(Coupons::getEndDate, LocalDate.now())
               .orderByAsc(Coupons::getBrandId);
        return couponsMapper.selectList(wrapper);
    }

    /**
     * 一次查多個品牌的有效券（is_active=true 且未過期）。
     * 給「附近店家 → 對應優惠券」
     *
     *
     * 組裝具體的 SQL 條件：*/
    public List<Coupons> selectActiveByBrandIds(Collection<Integer> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) return List.of();
        LambdaQueryWrapper<Coupons> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Coupons::getBrandId, brandIds)
               .eq(Coupons::getIsActive, true)
               .ge(Coupons::getEndDate, LocalDate.now())
               .orderByAsc(Coupons::getBrandId);

        // 呼叫 Mapper 去執行查詢
        return couponsMapper.selectList(wrapper);
    }


    // ── 增刪改（CRUD 的 C/U/D，R 上面已經有了）────────────────────────
    // insert 成功後，coupon.id 會被自動回填

    /** 新增一張券；成功會把自動產生的 id 寫回 coupon.id */
    public int insert(Coupons coupon) {
        return couponsMapper.insert(coupon);
    }

    /**
     * 依 id 更新。
     * 注意：MyBatis-Plus 預設「只更新非 null 欄位」，所以可以只傳要改的欄位就好。
     * 想清空某欄位的話要另外用 UpdateWrapper，這裡不處理。
     */
    public int updateById(Coupons coupon) {
        return couponsMapper.updateById(coupon);
    }

    /** 依 id 硬刪。資料庫沒設外鍵的話會真的不見，謹慎用 */
    public int deleteById(Integer id) {
        return couponsMapper.deleteById(id);
    }

    /** 切換收藏（愛心）：沒紀錄就新增一筆並設 liked=1，已存在就反轉 liked */
    public int toggleLiked(Integer userId, Integer couponId) {
        return couponsMapper.toggleLiked(userId, couponId);
    }

    // ── 收藏快取層（Redis）需要的三個查詢 ────────────────────

    /** 單筆收藏狀態；沒紀錄回 null */
    public Boolean selectLiked(Integer userId, Integer couponId) {
        return couponsMapper.selectLiked(userId, couponId);
    }

    /** 使用者目前收藏中的所有券 id（快取回填用） */
    public List<Integer> selectLikedCouponIds(Integer userId) {
        return couponsMapper.selectLikedCouponIds(userId);
    }

    /** 某張券的收藏總數（計數器重建用） */
    public long countLikes(Integer couponId) {
        return couponsMapper.countLikes(couponId);
    }
}