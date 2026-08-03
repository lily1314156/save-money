package com.example.demo.entity;

import lombok.Builder;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_coupons")
@Data
@Builder
public class UserCoupons {

    @TableId(type = IdType.AUTO)
    private Long          id;          // BIGINT，未來持有紀錄會很多，用 Long 不用 Integer

    @TableField("user_id")
    private Integer       userId;

    @TableField("coupon_id")
    private Integer       couponId;

    @TableField("liked")
    private Boolean       liked;       // 是否收藏

    @TableField("claimed_at")
    private LocalDateTime claimedAt;   // 領取時間
}