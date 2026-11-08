package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionNodeVO;
import com.yunshu.zhixue.modules.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-权限树（A-L04，仅超级管理员）。
 */
@Tag(name = "管理端-权限树")
@RestController
@RequestMapping("/api/admin/v1/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final RoleService roleService;

    @Operation(summary = "权限树")
    @GetMapping("/tree")
    public ApiResponse<List<PermissionNodeVO>> tree(@RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(roleService.tree(roleId));
    }
}
