package com.example.demo.mapper;
import com.example.demo.entity.Coupons;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CouponsMapper extends BaseMapper<Coupons> {

    /** 取得指定品牌（slug）所有啟用且未過期的券 */
    List<Coupons> selectAllBrandSlug(@Param("slug") String slug);

    /**
     * 當日限定券：CURDATE() 介於 start_date 與 end_date 之間且 is_active=1。
     * 額外 LEFT JOIN user_coupons 把該使用者的 liked 狀態帶出來，前端愛心要用。
     */
    List<Coupons> selectTodayCoupons(@Param("userId") Integer userId);

    //取得使用者持有的券，依 category 分類：
    List<Coupons> selectMyCoupons(@Param("userId") Integer userId,
                                  @Param("category") String category);

    @Insert("INSERT INTO user_coupons (user_id, coupon_id, liked) " +
        "VALUES (#{userId}, #{couponId}, 1) " +
        "ON DUPLICATE KEY UPDATE liked = 1 - liked")
    int toggleLiked(@Param("userId") Integer userId,
                    @Param("couponId") Integer couponId);

    // ── 以下三個查詢是給 Redis 快取層用的 ──────────────────

    /**
     * 查單筆收藏狀態。
     * 沒有紀錄回 null，有紀錄回 liked 欄位（true/false）。
     * toggle 完之後用它拿「翻轉後的最新狀態」。
     */
    @Select("SELECT liked FROM user_coupons " +
            "WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    Boolean selectLiked(@Param("userId") Integer userId,
                        @Param("couponId") Integer couponId);

    /**
     * 撈某使用者「目前收藏中」的所有券 id。
     * 快取 miss 時，用這份資料把整個 Redis Set 重建回來。
     */
    @Select("SELECT coupon_id FROM user_coupons " +
            "WHERE user_id = #{userId} AND liked = 1")
    List<Integer> selectLikedCouponIds(@Param("userId") Integer userId);

    /**
     * 某張券的收藏總數。
     * 計數器快取過期後，用它從 DB 重算（天然自我校正）。
     */
    @Select("SELECT COUNT(*) FROM user_coupons " +
            "WHERE coupon_id = #{couponId} AND liked = 1")
    long countLikes(@Param("couponId") Integer couponId);
}