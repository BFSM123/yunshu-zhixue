package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 当前登录用户上下文。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<LoginUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }
        return Optional.empty();
    }

    public static LoginUser requireUser() {
        return currentUser().orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }

    public static Long currentUserId() {
        return requireUser().id();
    }
}
