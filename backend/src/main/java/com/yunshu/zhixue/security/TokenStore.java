package com.yunshu.zhixue.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 令牌在 Redis 中的快照：权限码、访问令牌黑名单、刷新令牌白名单。
 *
 * <p>所有 key 的 TTL 与对应令牌剩余有效期一致，令牌过期后由 Redis 自动清理。</p>
 */
@Component
@RequiredArgsConstructor
public class TokenStore {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String PERMISSIONS_PREFIX = "token:permissions:";
    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String USER_VERSION_PREFIX = "token:user-version:";

    private final StringRedisTemplate redisTemplate;

    public void saveAccessSnapshot(String jti, Set<String> permissions, Duration ttl) {
        if (jti == null || isExpired(ttl)) {
            return;
        }
        String value = permissions == null ? "" : String.join(",", permissions);
        redisTemplate.opsForValue().set(PERMISSIONS_PREFIX + jti, value, ttl);
    }

    public Set<String> loadPermissions(String jti) {
        if (jti == null) {
            return Set.of();
        }
        String value = redisTemplate.opsForValue().get(PERMISSIONS_PREFIX + jti);
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>(Arrays.asList(value.split(",")));
        codes.removeIf(String::isBlank);
        return codes;
    }

    public void revokeAccessSnapshot(String jti) {
        if (jti != null) {
            redisTemplate.delete(PERMISSIONS_PREFIX + jti);
        }
    }

    public void blacklistAccess(String jti, Duration ttl) {
        if (jti == null || isExpired(ttl)) {
            return;
        }
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl);
    }

    public boolean isAccessBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public void activateRefresh(String jti, Long userId, Duration ttl) {
        if (jti == null || isExpired(ttl)) {
            return;
        }
        redisTemplate.opsForValue().set(REFRESH_PREFIX + jti, String.valueOf(userId), ttl);
    }

    public boolean isRefreshActive(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + jti));
    }

    public void revokeRefresh(String jti) {
        if (jti != null) {
            redisTemplate.delete(REFRESH_PREFIX + jti);
        }
    }

    /** 当前用户令牌版本号，未设置视为 0。 */
    public long currentVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String value = redisTemplate.opsForValue().get(USER_VERSION_PREFIX + userId);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** 递增用户令牌版本号，使该用户全部已签发令牌立即失效。 */
    public void bumpVersion(Long userId) {
        if (userId != null) {
            redisTemplate.opsForValue().increment(USER_VERSION_PREFIX + userId);
        }
    }

    /** 令牌版本号（缺失视为 0）。 */
    public static long versionOf(Claims claims) {
        Object value = claims.get("ver");
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * 令牌剩余有效期，已过期返回 {@link Duration#ZERO}。
     */
    public static Duration remaining(Claims claims) {
        Instant expiration = claims.getExpiration().toInstant();
        Duration ttl = Duration.between(Instant.now(), expiration);
        return ttl.isNegative() ? Duration.ZERO : ttl;
    }

    private boolean isExpired(Duration ttl) {
        return ttl == null || ttl.isZero() || ttl.isNegative();
    }
}
