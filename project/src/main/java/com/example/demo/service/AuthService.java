package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dao.UsersDao;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UsersDao usersDao;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 忘記密碼的重設 token → email 對照表 */
    private final ConcurrentHashMap<String, String> forgotPassword = new ConcurrentHashMap<>();

    public Map<String, Object> register(RegisterRequest request) {
        // email 是登入帳號，唯一性檢查改成查 email
        boolean exists = usersDao.existsByEmail(request.getEmail());
        if (exists) {
            return Map.of("failed", true, "message", "此 Email 已註冊過");
        }

        String password = passwordEncoder.encode(request.getPassword());

        Users user = Users.builder()
            .email(request.getEmail())
            .password(password)
            .username(request.getUsername())
            .createdAt(LocalDateTime.now())
            .build();
        usersDao.insert(user);
        return Map.of("success", true);
    }

    public String dologin(String email, String password) {
        Users user = usersDao.selectByEmail(email);

        if (user == null) {
            System.out.println("使用者不存在");
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("密碼錯誤");
            return null;
        }

        try {
            String str = objectMapper.writeValueAsString(user);
            String token = Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
            redisTemplate.opsForValue().set("token:" + token, user, 2, TimeUnit.HOURS);
            return token;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 拿 token 換使用者，查不到（沒登入或 token 過期）回 null */
    public Users getUserByToken(String token) {
        if (token == null) {
            return null;
        }
        return (Users) redisTemplate.opsForValue().get("token:" + token);
    }

    /** 登出：把 Redis 裡的 token 刪掉，這個 token 立刻失效 */
    public void logout(String token) {
        if (token != null) {
            redisTemplate.delete("token:" + token);
        }
    }

    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        Users user = usersDao.selectByEmail(request.getEmail());
        if (user == null) {
            return Map.of("success", false, "message", "查無此 Email");
        }

        String uuid = UUID.randomUUID().toString();
        forgotPassword.put(uuid, user.getEmail());
        return Map.of("success", true, "resetUrl", "/change-password?token=" + uuid);
    }

    public Map<String, Object> changePassword(ChangePasswordRequest request) {
        String email = forgotPassword.get(request.getToken());
        if (email == null) {
            return Map.of("success", false, "message", "無效的 token");
        }

        Users user = usersDao.selectByEmail(email);
        if (user == null) {
            return Map.of("success", false, "message", "查無此使用者");
        }

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        usersDao.updatePassword(user.getId(), hashedPassword);

        forgotPassword.remove(request.getToken());
        return Map.of("success", true);
    }
}