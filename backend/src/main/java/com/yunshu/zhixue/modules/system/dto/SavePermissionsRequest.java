package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 保存角色权限：全量覆盖。
 */
public record SavePermissionsRequest(
        @NotNull(message = "权限集合不能为空")
        List<Long> permissionIds
) {
}
