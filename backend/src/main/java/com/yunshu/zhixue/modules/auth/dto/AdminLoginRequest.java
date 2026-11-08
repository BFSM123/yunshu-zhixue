package com.yunshu.zhixue.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台登录入参：account 为用户名或手机号（Q4 定稿）。
 */
public record AdminLoginRequest(
        @NotBlank(message = "请输入用户名或手机号")
        @Size(max = 100, message = "账号长度不合法")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(max = 100, message = "密码长度不合法")
        String password
) {
}
