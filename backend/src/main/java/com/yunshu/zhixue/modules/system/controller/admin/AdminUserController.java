package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.CreateUserRequest;
import com.yunshu.zhixue.modules.system.dto.IdsRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResult;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.UpdateUserRequest;
import com.yunshu.zhixue.modules.system.dto.UserDetailVO;
import com.yunshu.zhixue.modules.system.dto.UserVO;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-用户管理（A-M01 ~ A-M08）。
 */
@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "用户列表")
    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(adminUserService.page(keyword, roleId, status, page, pageSize));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}/detail")
    @RequirePermission("user:view")
    public ApiResponse<UserDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.detail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @RequirePermission("user:add")
    public ApiResponse<UserVO> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(adminUserService.create(request, actorResolver.resolve()));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    public ApiResponse<UserVO> update(@PathVariable Long id,
                                      @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(adminUserService.update(id, request, actorResolver.resolve()));
    }

    @Operation(summary = "批量启用/禁用")
    @PutMapping("/batch-status")
    @RequirePermission("user:edit")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody BatchStatusRequest request) {
        adminUserService.batchStatus(request.ids(), request.status(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequirePermission("user:disable")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @RequirePermission("user:disable")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody IdsRequest request) {
        adminUserService.batchDelete(request.ids(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    public ApiResponse<ResetPasswordResult> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.resetPassword(id, actorResolver.resolve()));
    }
}
