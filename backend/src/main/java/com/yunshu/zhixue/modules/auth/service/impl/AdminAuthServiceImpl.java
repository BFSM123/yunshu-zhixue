package com.yunshu.zhixue.modules.auth.service.impl;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.LoginResult;
import com.yunshu.zhixue.modules.auth.service.AdminAuthService;
import com.yunshu.zhixue.modules.system.dto.AdminAccountView;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.JwtTokenProvider;
import com.yunshu.zhixue.security.TokenAudience;
import com.yunshu.zhixue.security.TokenStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 管理端认证实现：BCrypt 校验、失败锁定、双令牌签发与 Redis 快照。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final TokenAudience AUDIENCE = TokenAudience.ADMIN;
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOGIN_ACTION = "登录";
    private static final String LOGOUT_ACTION = "登出";
    private static final String TARGET_TYPE = "后台账号";

    private final AdminAccountService adminAccountService;
    private final OperationLogService operationLogService;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResult login(AdminLoginRequest request, String ip) {
        String account = request.account().trim();
        String failKey = LOGIN_FAIL_KEY_PREFIX + account;
        if (isLocked(failKey)) {
            throw new BizException(ErrorCode.ADMIN_LOCKED);
        }

        AdminAccountView accountView = adminAccountService.findByAccount(account).orElse(null);
        if (accountView == null || !passwordEncoder.matches(request.password(), accountView.passwordHash())) {
            int failures = increaseFailures(failKey);
            if (failures >= AppConstants.LOGIN_FAIL_LIMIT) {
                throw new BizException(ErrorCode.ADMIN_LOCKED);
            }
            throw new BizException(ErrorCode.ADMIN_BAD_CREDENTIALS);
        }

        if (!accountView.enabled()) {
            throw new BizException(ErrorCode.ADMIN_DISABLED);
        }

        redisTemplate.delete(failKey);
        adminAccountService.recordLogin(accountView.id());
        LoginResult result = issueTokens(accountView);
        operationLogService.record(accountView.id(), accountView.username(),
                LOGIN_ACTION, TARGET_TYPE, accountView.username(), ip);
        return result;
    }

    @Override
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = tokenProvider.parseRefreshToken(refreshToken, AUDIENCE);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String jti = claims.getId();
        if (!tokenStore.isRefreshActive(jti)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            tokenStore.revokeRefresh(jti);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (TokenStore.versionOf(claims) != tokenStore.currentVersion(userId)) {
            tokenStore.revokeRefresh(jti);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccountView account = adminAccountService.findById(userId)
                .filter(AdminAccountView::enabled)
                .orElse(null);
        if (account == null) {
            tokenStore.revokeRefresh(jti);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        tokenStore.revokeRefresh(jti);
        return issueTokens(account);
    }

    @Override
    public void logout(String accessToken, String refreshToken, String ip) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = tokenProvider.parseAccessToken(accessToken, AUDIENCE);
                String jti = claims.getId();
                tokenStore.blacklistAccess(jti, TokenStore.remaining(claims));
                tokenStore.revokeAccessSnapshot(jti);
                adminAccountService.findById(Long.valueOf(claims.getSubject())).ifPresent(account ->
                        operationLogService.record(account.id(), account.username(),
                                LOGOUT_ACTION, TARGET_TYPE, account.username(), ip));
            } catch (Exception ignored) {
                // 登出幂等：令牌无效或已过期时仍返回成功
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = tokenProvider.parseRefreshToken(refreshToken, AUDIENCE);
                tokenStore.revokeRefresh(claims.getId());
            } catch (Exception ignored) {
                // 刷新令牌无效同样忽略
            }
        }
    }

    private LoginResult issueTokens(AdminAccountView account) {
        long version = tokenStore.currentVersion(account.id());
        String accessToken = tokenProvider.createAccessToken(account.id(), AUDIENCE, version);
        String refreshToken = tokenProvider.createRefreshToken(account.id(), AUDIENCE, version);

        Claims accessClaims = tokenProvider.parseAccessToken(accessToken, AUDIENCE);
        Claims refreshClaims = tokenProvider.parseRefreshToken(refreshToken, AUDIENCE);
        tokenStore.saveAccessSnapshot(accessClaims.getId(), account.permissions(),
                TokenStore.remaining(accessClaims));
        tokenStore.activateRefresh(refreshClaims.getId(), account.id(),
                TokenStore.remaining(refreshClaims));

        return new LoginResult(accessToken, refreshToken, account.permissions(),
                account.roleCode(), account.username(), account.username());
    }

    private boolean isLocked(String failKey) {
        String value = redisTemplate.opsForValue().get(failKey);
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= AppConstants.LOGIN_FAIL_LIMIT;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int increaseFailures(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        int failures = count == null ? 1 : count.intValue();
        if (failures == 1 || failures >= AppConstants.LOGIN_FAIL_LIMIT) {
            redisTemplate.expire(failKey, Duration.ofMinutes(AppConstants.LOGIN_LOCK_MINUTES));
        }
        return failures;
    }
}
