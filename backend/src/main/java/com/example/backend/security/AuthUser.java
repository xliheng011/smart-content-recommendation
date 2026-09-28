package com.example.backend.security;

/**
 * 当前登录用户（由 token 解析而来）。
 */
public record AuthUser(
        Long userId,
        String username,
        String role
) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
