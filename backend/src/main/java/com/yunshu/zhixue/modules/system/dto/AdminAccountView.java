package com.yunshu.zhixue.modules.system.dto;

import java.util.Set;

/**
 * 登录认证使用的后台账号视图（含角色编码与权限码）。
 */
public record AdminAccountView(
        Long id,
        String username,
        String passwordHash,
        String roleCode,
        boolean enabled,
        Set<String> permissions
) {
}
