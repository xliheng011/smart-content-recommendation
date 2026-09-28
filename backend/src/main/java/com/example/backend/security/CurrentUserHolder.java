package com.example.backend.security;

/**
 * 请求级当前用户上下文。
 *
 * 由 AuthInterceptor 在请求进入时写入，请求结束时清理，
 * 避免 ThreadLocal 在复用线程池时串数据。
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {

        AuthUser user = HOLDER.get();

        return user == null ? null : user.userId();
    }

    public static boolean isAdmin() {

        AuthUser user = HOLDER.get();

        return user != null && user.isAdmin();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
