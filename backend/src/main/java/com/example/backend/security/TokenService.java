package com.example.backend.security;

import com.example.backend.service.CacheService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量会话令牌服务。
 *
 * 登录成功后签发随机 token，服务端保存 token -> 用户 的映射；
 * Redis 可用时存 Redis（支持多实例），否则退化为进程内 Map。
 */
@Service
public class TokenService {

    public static final String TOKEN_PREFIX = "auth:token:";

    /** 令牌有效期：7 天 */
    public static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final CacheService cacheService;

    private final Map<String, LocalToken> localTokens =
            new ConcurrentHashMap<>();

    public TokenService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 签发令牌。
     */
    public String issue(AuthUser user) {

        String token = UUID.randomUUID()
                .toString()
                .replace("-", "");

        cacheService.set(
                TOKEN_PREFIX + token,
                user,
                TOKEN_TTL
        );

        // 即使 Redis 正常，也保留一份本地副本，
        // 便于 Redis 抖动时仍能识别已登录用户
        localTokens.put(
                token,
                new LocalToken(user, System.currentTimeMillis()
                        + TOKEN_TTL.toMillis())
        );

        purgeExpiredLocal();

        return token;
    }

    /**
     * 解析令牌。
     */
    public Optional<AuthUser> resolve(String token) {

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<AuthUser> cached = cacheService.get(
                TOKEN_PREFIX + token,
                AuthUser.class
        );

        if (cached.isPresent()) {
            return cached;
        }

        LocalToken local = localTokens.get(token);

        if (local == null) {
            return Optional.empty();
        }

        if (local.isExpired()) {
            localTokens.remove(token);
            return Optional.empty();
        }

        return Optional.of(local.user());
    }

    /**
     * 注销令牌。
     */
    public void revoke(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        localTokens.remove(token);
        cacheService.delete(TOKEN_PREFIX + token);
    }

    private void purgeExpiredLocal() {

        localTokens.entrySet().removeIf(
                entry -> entry.getValue().isExpired()
        );
    }

    private record LocalToken(AuthUser user, long expireAt) {

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
