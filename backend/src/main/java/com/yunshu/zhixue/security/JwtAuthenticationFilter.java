package com.yunshu.zhixue.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 双端 JWT 认证过滤器：按请求路径前缀判定端标识，解析失败不写响应，
 * 由 {@link RestAuthenticationEntryPoint} 统一返回 401 + 10001。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        TokenAudience audience = TokenAudience.fromRequestPath(request.getRequestURI());
        try {
            Claims claims = tokenProvider.parseAccessToken(token, audience);
            String jti = claims.getId();
            if (tokenStore.isAccessBlacklisted(jti)) {
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            if (TokenStore.versionOf(claims) != tokenStore.currentVersion(userId)) {
                return;
            }
            // 权限码在登录时按角色加载并写入 Redis 令牌快照。
            Set<String> permissions = tokenStore.loadPermissions(jti);
            LoginUser principal = new LoginUser(userId, audience, permissions);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT 校验失败: {}", ex.getMessage());
        }
    }
}
