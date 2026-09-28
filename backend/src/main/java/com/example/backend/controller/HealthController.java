package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.service.CacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：同时反馈数据库与缓存状态，便于快速定位环境问题。
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    private final CacheService cacheService;

    public HealthController(
            DataSource dataSource,
            CacheService cacheService
    ) {
        this.dataSource = dataSource;
        this.cacheService = cacheService;
    }

    /**
     * GET /api/health
     */
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {

        Map<String, Object> status = new LinkedHashMap<>();

        status.put("service", "up");
        status.put("database", databaseStatus());
        status.put("cache", cacheService.isRedisAvailable()
                ? "redis"
                : "local-memory");
        status.put("timestamp", System.currentTimeMillis());

        return ApiResponse.ok(status);
    }

    private String databaseStatus() {

        try (Connection connection = dataSource.getConnection()) {

            return connection.isValid(2)
                    ? "up"
                    : "down";

        } catch (Exception ex) {
            return "down: " + ex.getMessage();
        }
    }
}
