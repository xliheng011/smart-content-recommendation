package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存门面。
 *
 * 设计目标：
 * 1. 优先使用 Redis；
 * 2. Redis 不可用（未部署 / 宕机 / 超时）时自动降级为进程内缓存，
 *    业务代码不需要写任何 try-catch，也不会因为缓存问题导致接口 500；
 * 3. 失败后进入冷却期，避免每次请求都去等待连接超时。
 */
@Service
public class CacheService {

    private static final Logger log =
            LoggerFactory.getLogger(CacheService.class);

    /** Redis 失败后的冷却时间，冷却期内直接走本地缓存 */
    private static final Duration FAILURE_COOLDOWN = Duration.ofSeconds(30);

    /** 本地兜底缓存的最大条目数，防止无界增长 */
    private static final int LOCAL_MAX_ENTRIES = 2000;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final Map<String, LocalEntry> localCache =
            new ConcurrentHashMap<>();

    private volatile boolean redisAvailable = true;

    private volatile long redisRetryAt = 0L;

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 启动后探测一次 Redis，便于在日志中直观看到缓存是否生效。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void probeOnStartup() {

        if (ping()) {
            log.info("缓存模式：Redis（分布式缓存已启用）");
        } else {
            log.warn("缓存模式：本地内存（Redis 不可用，已自动降级，业务不受影响）");
        }
    }

    public boolean ping() {

        try {

            String pong = redisTemplate.execute(
                    (RedisCallback<String>) connection -> connection.ping()
            );

            boolean ok = pong != null;

            if (ok) {
                redisAvailable = true;
                redisRetryAt = 0L;
            }

            return ok;

        } catch (Exception ex) {
            markUnavailable(ex);
            return false;
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable && System.currentTimeMillis() >= redisRetryAt;
    }

    /* ============================
       读
    ============================ */

    public <T> Optional<T> get(String key, Class<T> type) {

        if (isRedisAvailable()) {

            try {

                String json = redisTemplate.opsForValue().get(key);

                if (json != null) {
                    return Optional.ofNullable(
                            objectMapper.readValue(json, type)
                    );
                }

            } catch (Exception ex) {
                markUnavailable(ex);
            }
        }

        return readLocal(key, type);
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {

        if (isRedisAvailable()) {

            try {

                String json = redisTemplate.opsForValue().get(key);

                if (json != null) {
                    return Optional.ofNullable(
                            objectMapper.readValue(json, type)
                    );
                }

            } catch (Exception ex) {
                markUnavailable(ex);
            }
        }

        return readLocal(key, type);
    }

    /* ============================
       写
    ============================ */

    public void set(String key, Object value, Duration ttl) {

        if (value == null) {
            return;
        }

        String json;

        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("缓存序列化失败，已跳过写入: {}", key);
            return;
        }

        if (isRedisAvailable()) {

            try {

                if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, json);
                } else {
                    redisTemplate.opsForValue().set(key, json, ttl);
                }

                return;

            } catch (Exception ex) {
                markUnavailable(ex);
            }
        }

        writeLocal(key, json, ttl);
    }

    /* ============================
       删
    ============================ */

    public void delete(String key) {

        localCache.remove(key);

        if (!isRedisAvailable()) {
            return;
        }

        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            markUnavailable(ex);
        }
    }

    /**
     * 按前缀批量失效，用于推荐结果这类成组缓存。
     */
    public void deleteByPrefix(String prefix) {

        localCache.keySet().removeIf(key -> key.startsWith(prefix));

        if (!isRedisAvailable()) {
            return;
        }

        try {

            Set<String> keys = new HashSet<>();

            ScanOptions options = ScanOptions
                    .scanOptions()
                    .match(prefix + "*")
                    .count(200)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {

                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

        } catch (Exception ex) {
            markUnavailable(ex);
        }
    }

    /* ============================
       本地缓存实现
    ============================ */

    private <T> Optional<T> readLocal(String key, Class<T> type) {

        String json = readLocalJson(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(
                    objectMapper.readValue(json, type)
            );
        } catch (Exception ex) {
            localCache.remove(key);
            return Optional.empty();
        }
    }

    private <T> Optional<T> readLocal(
            String key,
            TypeReference<T> type
    ) {

        String json = readLocalJson(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(
                    objectMapper.readValue(json, type)
            );
        } catch (Exception ex) {
            localCache.remove(key);
            return Optional.empty();
        }
    }

    private String readLocalJson(String key) {

        LocalEntry entry = localCache.get(key);

        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            localCache.remove(key);
            return null;
        }

        return entry.json();
    }

    private void writeLocal(String key, String json, Duration ttl) {

        if (localCache.size() >= LOCAL_MAX_ENTRIES) {
            purgeExpired();
        }

        long expireAt = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + ttl.toMillis();

        localCache.put(key, new LocalEntry(json, expireAt));
    }

    private void purgeExpired() {

        List<String> expired = new ArrayList<>();

        localCache.forEach((key, entry) -> {
            if (entry.isExpired()) {
                expired.add(key);
            }
        });

        expired.forEach(localCache::remove);

        // 仍然过大则直接清空，宁可回源数据库也不让内存无界增长
        if (localCache.size() >= LOCAL_MAX_ENTRIES) {
            localCache.clear();
        }
    }

    private void markUnavailable(Exception ex) {

        if (redisAvailable) {
            log.warn(
                    "Redis 访问失败，已切换为本地缓存（{} 秒后重试）: {}",
                    FAILURE_COOLDOWN.toSeconds(),
                    ex.getMessage()
            );
        }

        redisAvailable = false;
        redisRetryAt = System.currentTimeMillis()
                + FAILURE_COOLDOWN.toMillis();
    }

    private record LocalEntry(String json, long expireAt) {

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
