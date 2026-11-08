package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 后台用户列表 / 详情视图。
 */
public record UserVO(
        Long id,
        String username,
        String mobile,
        String email,
        Long roleId,
        String roleName,
        String roleCode,
        Integer status,
        Boolean isBuiltin,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
