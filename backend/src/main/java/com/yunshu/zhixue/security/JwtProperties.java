package com.yunshu.zhixue.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 双端 JWT 配置：各自独立的签发密钥与过期策略。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private TokenConfig portal = new TokenConfig();
    private TokenConfig admin = new TokenConfig();

    @Data
    public static class TokenConfig {
        private String issuer = "yunshu-zhixue";
        private String secret;
        private Duration accessTtl = Duration.ofHours(2);
        private Duration refreshTtl = Duration.ofDays(14);
    }

    public TokenConfig of(TokenAudience audience) {
        return audience == TokenAudience.ADMIN ? admin : portal;
    }
}
