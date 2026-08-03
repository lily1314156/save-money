package com.example.demo.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.entity.Users;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/user/email")
    public Map<String, Object> getUserEmail(@CookieValue(value = "token", required = false) String token, HttpServletResponse response) {

        Users user = authService.getUserByToken(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "未登入");
        }
        return Map.of("email", user.getEmail());
    }
}