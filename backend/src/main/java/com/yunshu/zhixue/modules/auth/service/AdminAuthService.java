package com.yunshu.zhixue.modules.auth.service;

import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.LoginResult;

/**
 * 管理端认证：登录、刷新令牌、登出。
 */
public interface AdminAuthService {

    LoginResult login(AdminLoginRequest request, String ip);

    LoginResult refresh(String refreshToken);

    void logout(String accessToken, String refreshToken, String ip);
}
