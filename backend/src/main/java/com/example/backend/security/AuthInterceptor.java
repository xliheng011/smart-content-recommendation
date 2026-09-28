package com.example.backend.security;

import com.example.backend.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 认证拦截器。
 *
 * 1. 解析 Authorization / X-Token，把当前用户写入上下文；
 * 2. 对 /api/admin/** 强制要求管理员身份；
 * 3. 请求结束后清理 ThreadLocal。
 *
 * 其余接口保持开放，控制器可以通过 CurrentUserHolder 拿到"可选的"当前用户，
 * 从而在未登录时也能返回内容（只是不带个性化字段）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PATH_PREFIX = "/api/admin";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        resolveToken(request)
                .flatMap(tokenService::resolve)
                .ifPresent(CurrentUserHolder::set);

        if (requiresAdmin(request) && !CurrentUserHolder.isAdmin()) {

            AuthUser current = CurrentUserHolder.get();

            throw current == null
                    ? ApiException.unauthorized("请先登录后再操作")
                    : ApiException.forbidden("当前账号没有管理员权限");
        }

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        CurrentUserHolder.clear();
    }

    private boolean requiresAdmin(HttpServletRequest request) {

        String path = request.getRequestURI();

        return path != null && path.startsWith(ADMIN_PATH_PREFIX);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {

        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && !authorization.isBlank()
                && authorization.regionMatches(
                        true,
                        0,
                        "Bearer ",
                        0,
                        7
                )) {

            String token = authorization.substring(7).trim();

            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }

        String header = request.getHeader("X-Token");

        if (header != null && !header.isBlank()) {
            return Optional.of(header.trim());
        }

        String param = request.getParameter("token");

        if (param != null && !param.isBlank()) {
            return Optional.of(param.trim());
        }

        return Optional.empty();
    }
}
