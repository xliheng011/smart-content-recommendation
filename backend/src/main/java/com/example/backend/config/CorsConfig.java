package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置。
 *
 * 允许的来源通过 app.cors.allowed-origins 配置，逗号分隔，
 * 默认覆盖 Vite 常用的 5173 / 5174 端口。
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        // 使用 originPatterns，既支持精确地址也支持通配写法
        config.setAllowedOriginPatterns(origins);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        config.setAllowCredentials(true);

        // 让前端可以读取自定义响应头（便于排查问题）
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-Total-Count");

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
