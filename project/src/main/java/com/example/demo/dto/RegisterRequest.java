package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用戶名不能為空")
    private String username;

    @NotBlank
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank
    private String password;
}