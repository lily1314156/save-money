package com.example.demo.controller;

import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/register")
    @ResponseBody
    public Map<String, Object> register(@Validated @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/api/forgot-password")
    @ResponseBody
    public Map<String, Object> forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/api/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    @PostMapping("/api/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String email = request.get("email");
        String password = request.get("password");

        String token = authService.dologin(email, password);
        if (token != null) {
            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return Map.of("success", true);
        } else {
            return Map.of("success", false, "message", "帳號或密碼錯誤");
        }
    }

    @PostMapping("/api/logout")
    @ResponseBody
    public Map<String, Object> logout(@CookieValue(value = "token", required = false) String token,
                                      HttpServletResponse response) {
        // 1. 刪掉 Redis 裡的 token，讓它失效
        authService.logout(token);

        // 2. 清掉瀏覽器的 cookie：同名、同 path，maxAge 設 0 = 叫瀏覽器立刻刪除
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return Map.of("success", true);
    }
}