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

@TableName("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @TableId(type = IdType.AUTO)
    private Integer       id;

    @TableField("email")
    private String        email;

    @TableField("password")
    private String        password;  // 之後用 BCrypt 加密後存入，不存明碼

    @TableField("username")
    private String        username;

    @TableField("created_at")
    private LocalDateTime createdAt;
}