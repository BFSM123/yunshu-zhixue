package com.yunshu.zhixue.modules.auth.dto;

import java.util.Set;

/**
 * 后台登录 / 刷新令牌的统一返回。
 */
public record LoginResult(
        String accessToken,
        String refreshToken,
        Set<String> permissions,
        String roleCode,
        String username,
        String nickname
) {
}
