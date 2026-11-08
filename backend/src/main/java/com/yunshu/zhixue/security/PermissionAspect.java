package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 权限切面：按当前管理员的权限码集合校验 {@link RequirePermission}。
 * 无权限返回 403 + 10002。
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        LoginUser user = SecurityUtils.requireUser();
        if (user.audience() != TokenAudience.ADMIN) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!user.hasPermission(requirePermission.value())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
