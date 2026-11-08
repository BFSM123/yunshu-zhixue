package com.yunshu.zhixue.modules.auth.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.LoginResult;
import com.yunshu.zhixue.modules.auth.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口（A-A01 ~ A-A03）。
 */
@Tag(name = "管理端认证")
@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminAuthService adminAuthService;

    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody AdminLoginRequest request,
                                          HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminAuthService.login(request, clientIp(servletRequest)));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<LoginResult> refresh(
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshToken) {
        return ApiResponse.ok(adminAuthService.refresh(refreshToken));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(name = "X-Refresh-Token", required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        adminAuthService.logout(bearerToken(authorization), refreshToken, clientIp(servletRequest));
        return ApiResponse.ok();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : authorization;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
