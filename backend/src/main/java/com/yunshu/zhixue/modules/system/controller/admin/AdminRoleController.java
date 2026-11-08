package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.CreateRoleRequest;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.dto.RoleVO;
import com.yunshu.zhixue.modules.system.dto.SavePermissionsRequest;
import com.yunshu.zhixue.modules.system.service.RoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-角色权限（A-L01 ~ A-L05，仅超级管理员）。
 */
@Tag(name = "管理端-角色权限")
@RestController
@RequestMapping("/api/admin/v1/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "角色列表")
    @GetMapping
    public ApiResponse<List<RoleVO>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public ApiResponse<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.ok(roleService.create(request, actorResolver.resolve()));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "保存角色权限")
    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> savePermissions(@PathVariable Long id,
                                             @Valid @RequestBody SavePermissionsRequest request) {
        roleService.savePermissions(id, request.permissionIds(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    /**
     * 用户管理页角色下拉，非超管操作员也可读取。
     */
    @Operation(summary = "角色下拉选项")
    @GetMapping("/options")
    @RequirePermission("user:view")
    public ApiResponse<List<RoleOptionVO>> options() {
        return ApiResponse.ok(roleService.options());
    }
}
