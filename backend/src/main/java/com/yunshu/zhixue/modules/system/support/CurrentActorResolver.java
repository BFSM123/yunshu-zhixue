package com.yunshu.zhixue.modules.system.support;

import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import com.yunshu.zhixue.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 解析当前操作人（用户 ID、用户名快照、IP），供审计日志使用。
 */
@Component
@RequiredArgsConstructor
public class CurrentActorResolver {

    private final AdminAccountService adminAccountService;

    public Actor resolve() {
        Long userId = SecurityUtils.currentUserId();
        String username = adminAccountService.findById(userId)
                .map(account -> account.username())
                .orElse("未知用户");
        return new Actor(userId, username, clientIp());
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "-";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
