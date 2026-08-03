package com.example.demo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.demo.entity.Users;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

         // 第一步：從 Cookie 取出 Token
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 第二步：拿 Token 去 Service 查有沒有對應使用者
        Users user = authService.getUserByToken(token);
        if (user == null) {
            // API 請求（fetch）→ 回 401 狀態碼，讓前端 JS 自己判斷、跳提醒
            // 頁面請求（瀏覽器）→ 轉址到登入頁
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            } else {
                response.sendRedirect("/login");
            }
            return false;
        }

        // 第三步：驗證通過，把 user 掛在這次 request 上，
        // 後面的 Controller 可以用 @RequestAttribute 直接拿到，不用再查一次
        request.setAttribute("loginUser", user);
        return true;
    }
}