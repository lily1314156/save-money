package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("stores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stores {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("brand_id")
    private Integer brandId;

    @TableField("name")
    private String  name;           // 分店顯示名（例：50嵐 黎明店）

    @TableField("address")
    private String  address;

    @TableField("business_hours")
    private String  businessHours;

    @TableField("phone")
    private String  phone;


    @TableField("is_active")
    private Boolean isActive;

    // ── 經緯度 ──
    @TableField("lat")
    private Double lat;

    @TableField("lng")
    private Double lng;

    @TableField("geocoded_at")
    private LocalDateTime geocodedAt; // 上次跟 Google 要這筆經緯度的時間（給快取過期用）
}