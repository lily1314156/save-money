package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo.typehandler.JsonStringListTypeHandler;
import java.util.List;
import java.math.BigDecimal;

@TableName(value = "coupons", autoResultMap = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupons {

    @TableId(type = IdType.AUTO)
    private Integer    id;

    @TableField("brand_id")
    private Integer    brandId;

    @TableField("type")
    private String     type;

    @TableField("title")
    private String     title;

    // 期限：用 LocalDate
    @TableField("start_date")
    private LocalDate  startDate;

    @TableField("end_date")
    private LocalDate  endDate;

    // 價格型專用
    @TableField("discount")
    private int discount;

    @TableField("price_tag")
    private String     priceTag;      // 優惠卷 / 折扣卷

    @TableField("content")
    private String     content; // 消費條件文字

    // 買一送一型專用
    @TableField("deal_text")
    private String     dealText;     // 「買一送一」


    /**
     * 資料庫是 JSON 欄位。
     * 透過 JsonStringListTypeHandler 自動在 JSON 字串 ↔ List<String> 之間轉換，
     * 對應的 typeHandler 設定寫在 CouponMapper.xml 的 ResultMap 裡。
     */
    @TableField(value = "terms", typeHandler = JsonStringListTypeHandler.class)
    private List<String> terms;

    @TableField("is_active")
    private Boolean    isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 是否被當前使用者收藏。
     * 注意：只有透過 findMyCoupons（join user_coupons）出來的結果才會有值，
     *      其他查詢（findActiveByBrandSlug、findById）這個欄位是 null。
     */
    @TableField(exist = false)
    private Boolean    liked;
}