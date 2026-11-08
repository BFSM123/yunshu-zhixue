package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增后台用户。
 */
public record CreateUserRequest(
        @NotBlank(message = "请输入用户名")
        @Size(min = 2, max = 50, message = "用户名长度为 2~50 位")
        String username,

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 位")
        String email,

        @NotNull(message = "请选择角色")
        Long roleId,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度为 6~20 位")
        String password,

        Integer status
) {
}
