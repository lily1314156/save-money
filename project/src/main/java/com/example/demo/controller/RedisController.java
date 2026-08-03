package com.example.demo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/redis")
@AllArgsConstructor
public class RedisController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public Object set(@RequestParam String value) {
        redisTemplate.opsForValue().set("value", value, 60L, java.util.concurrent.TimeUnit.SECONDS);
        return "ok";
    }

    @GetMapping("/get")
    public Object get() {
        return redisTemplate.opsForValue().get("value");
    }
}