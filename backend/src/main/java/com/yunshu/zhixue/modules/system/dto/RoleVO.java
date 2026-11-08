package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 角色卡片视图。
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        String description,
        long userCount,
        List<String> permissionLabels
) {
}
