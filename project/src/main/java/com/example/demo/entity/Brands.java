package com.example.demo.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("brands")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brands {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String  name;       // 顯示用品牌名（例：50嵐）

    @TableField("slug")
    private String  slug;       // 對應原本前端用的 id（例：fifty）

    @TableField("bg_color")
    private String  bgColor; 

    @TableField("icon")
    private String  icon;

    @TableField("sort_order")
    private Integer sortOrder;  

    @TableField("is_active")
    private Boolean isActive; 

    @TableField("aliases")
    private String  aliases;    // 品牌別名，逗號分隔（例："麥當勞,McDonald's,mcdonalds"）
                                // 讓 isPlaceMatchingBrand 能比對更多變體，null 代表不設別名

    @TableField("place_type")
    private String  placeType;  // Google Places 的 type（例：convenience_store），
                                // Text Search 時用來過濾類別；null = 不過濾
                                // 只設分類穩定的（超商/咖啡/速食），手搖飲不設以免查不到
}