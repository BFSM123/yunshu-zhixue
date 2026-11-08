package com.yunshu.zhixue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 *
 * <p>access/refresh 均带 {@code aud} 端标识与 {@code type} 类型声明；
 * 解析时强制校验端标识，防止学员令牌访问管理端接口。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;

    public String createAccessToken(Long userId, TokenAudience audience) {
        return createAccessToken(userId, audience, 0L);
    }

    public String createAccessToken(Long userId, TokenAudience audience, long version) {
        return createToken(userId, audience, TYPE_ACCESS,
                properties.of(audience).getAccessTtl().toMillis(), version);
    }

    public String createRefreshToken(Long userId, TokenAudience audience) {
        return createRefreshToken(userId, audience, 0L);
    }

    public String createRefreshToken(Long userId, TokenAudience audience, long version) {
        return createToken(userId, audience, TYPE_REFRESH,
                properties.of(audience).getRefreshTtl().toMillis(), version);
    }

    public Claims parseAccessToken(String token, TokenAudience audience) {
        return parse(token, audience, TYPE_ACCESS);
    }

    public Claims parseRefreshToken(String token, TokenAudience audience) {
        return parse(token, audience, TYPE_REFRESH);
    }

    public long accessTokenTtlSeconds(TokenAudience audience) {
        return properties.of(audience).getAccessTtl().toSeconds();
    }

    private String createToken(Long userId, TokenAudience audience, String type,
                               long ttlMillis, long version) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.of(audience).getIssuer())
                .audience().add(audience.value()).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .claim("type", type)
                .claim("ver", version)
                .signWith(keyOf(audience), Jwts.SIG.HS256)
                .compact();
    }

    private Claims parse(String token, TokenAudience audience, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(keyOf(audience))
                .requireAudience(audience.value())
                .requireIssuer(properties.of(audience).getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("令牌类型不匹配");
        }
        return claims;
    }

    private SecretKey keyOf(TokenAudience audience) {
        String secret = properties.of(audience).getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥未配置或长度不足 32 字节: " + audience.value());
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
