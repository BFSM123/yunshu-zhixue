package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增角色。
 */
public record CreateRoleRequest(
        @NotBlank(message = "请输入角色名称")
        @Size(max = 50, message = "角色名称不能超过 50 位")
        String name,

        @NotBlank(message = "请输入角色编码")
        @Size(max = 50, message = "角色编码不能超过 50 位")
        @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                message = "角色编码仅支持小写字母、数字和下划线，且以字母开头")
        String code,

        @Size(max = 255, message = "角色描述不能超过 255 位")
        String description
) {
}
