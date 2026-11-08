package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SettingsVO;
import com.yunshu.zhixue.modules.system.dto.UpdateSettingsRequest;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-系统设置（A-T01 ~ A-T03）。
 */
@Tag(name = "管理端-系统设置")
@RestController
@RequestMapping("/api/admin/v1/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SystemSettingService systemSettingService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "读取系统设置")
    @GetMapping
    public ApiResponse<SettingsVO> get() {
        return ApiResponse.ok(systemSettingService.get());
    }

    @Operation(summary = "保存系统设置")
    @PutMapping
    @RequirePermission("system:setting")
    public ApiResponse<Void> update(@Valid @RequestBody UpdateSettingsRequest request) {
        systemSettingService.update(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "恢复默认设置")
    @PostMapping("/reset")
    @RequirePermission("system:setting")
    public ApiResponse<Void> reset() {
        systemSettingService.reset(actorResolver.resolve());
        return ApiResponse.ok();
    }
}
