package com.example.backend.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redis")
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    public RedisTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/test")
    public String setValue(
            @RequestParam String key,
            @RequestParam String value) {

        redisTemplate.opsForValue().set(key, value);
        return "Redis 写入成功";
    }

    @GetMapping("/test")
    public String getValue(@RequestParam String key) {

        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return "Key 不存在";
        }

        return value;
    }
}