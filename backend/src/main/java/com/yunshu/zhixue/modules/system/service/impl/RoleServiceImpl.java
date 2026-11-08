package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.AdminAccountView;
import com.yunshu.zhixue.modules.system.dto.CreateRoleRequest;
import com.yunshu.zhixue.modules.system.dto.PermissionNodeVO;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.dto.RoleVO;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final String SUPER_ADMIN_CODE = "super_admin";

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminAccountService adminAccountService;
    private final OperationLogService operationLogService;

    @Override
    public List<RoleVO> list() {
        assertSuperAdmin();
        List<Role> roles = roleMapper.selectList(
                Wrappers.lambdaQuery(Role.class).orderByAsc(Role::getId));
        List<Permission> permissions = permissionMapper.selectList(
                Wrappers.lambdaQuery(Permission.class)
                        .orderByAsc(Permission::getSortOrder)
                        .orderByAsc(Permission::getId));
        Map<Long, Permission> permissionById = permissions.stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));
        Map<Long, Set<Long>> rolePermissionIds = rolePermissionMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(RolePermission::getRoleId,
                        Collectors.mapping(RolePermission::getPermissionId, Collectors.toSet())));

        return roles.stream().map(role -> {
            long userCount = adminUserMapper.selectCount(
                    Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getRoleId, role.getId()));
            Set<Long> granted = rolePermissionIds.getOrDefault(role.getId(), Set.of());
            List<String> labels = buildPermissionLabels(role, granted, permissions, permissionById);
            return new RoleVO(role.getId(), role.getName(), role.getCode(), role.getDescription(),
                    userCount, labels);
        }).toList();
    }

    @Override
    public RoleVO create(CreateRoleRequest request, Actor actor) {
        assertSuperAdmin();
        String code = request.code().trim();
        long exists = roleMapper.selectCount(
                Wrappers.lambdaQuery(Role.class).eq(Role::getCode, code));
        if (exists > 0) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "角色编码已存在");
        }

        Role role = new Role();
        role.setName(request.name().trim());
        role.setCode(code);
        role.setDescription(normalizeText(request.description()));
        roleMapper.insert(role);
        audit(actor, "新增", role.getName());
        return new RoleVO(role.getId(), role.getName(), role.getCode(), role.getDescription(),
                0, List.of("暂无权限"));
    }

    @Override
    public void delete(Long id, Actor actor) {
        assertSuperAdmin();
        Role role = requireRole(id);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "超级管理员角色不可删除");
        }
        long userCount = adminUserMapper.selectCount(
                Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getRoleId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.ROLE_HAS_USERS);
        }
        roleMapper.deleteById(id);
        audit(actor, "删除", role.getName());
    }

    @Override
    public List<PermissionNodeVO> tree(Long roleId) {
        assertSuperAdmin();
        Role role = roleId == null ? null : requireRole(roleId);
        List<Permission> permissions = permissionMapper.selectList(
                Wrappers.lambdaQuery(Permission.class)
                        .orderByAsc(Permission::getSortOrder)
                        .orderByAsc(Permission::getId));
        Set<Long> granted = role == null
                ? Set.of()
                : permissionMapper.selectByRoleId(role.getId()).stream()
                        .map(Permission::getId)
                        .collect(Collectors.toSet());
        boolean allChecked = role != null && SUPER_ADMIN_CODE.equals(role.getCode());

        Map<Long, List<Permission>> childrenByParent = permissions.stream()
                .filter(permission -> permission.getParentId() != null)
                .collect(Collectors.groupingBy(Permission::getParentId));

        return permissions.stream()
                .filter(permission -> permission.getParentId() == null)
                .map(group -> {
                    List<PermissionNodeVO> children = childrenByParent
                            .getOrDefault(group.getId(), List.of()).stream()
                            .map(child -> new PermissionNodeVO(child.getId(), child.getName(),
                                    child.getCode(), allChecked || granted.contains(child.getId()), List.of()))
                            .toList();
                    if (children.isEmpty()) {
                        return new PermissionNodeVO(group.getId(), group.getName(), group.getCode(),
                                allChecked || granted.contains(group.getId()), List.of());
                    }
                    boolean checkedChildren = children.stream().allMatch(PermissionNodeVO::checked);
                    return new PermissionNodeVO(group.getId(), group.getName(), group.getCode(),
                            allChecked || granted.contains(group.getId()) || checkedChildren, children);
                })
                .toList();
    }

    @Override
    @Transactional
    public void savePermissions(Long roleId, List<Long> permissionIds, Actor actor) {
        assertSuperAdmin();
        Role role = requireRole(roleId);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "超级管理员角色权限不可编辑");
        }

        List<Long> distinctIds = permissionIds == null
                ? List.of()
                : permissionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinctIds.isEmpty()) {
            long existing = permissionMapper.selectCount(
                    Wrappers.lambdaQuery(Permission.class).in(Permission::getId, distinctIds));
            if (existing != distinctIds.size()) {
                throw new BizException(ErrorCode.PERMISSION_SAVE_FAILED);
            }
        }

        rolePermissionMapper.delete(Wrappers.lambdaQuery(RolePermission.class)
                .eq(RolePermission::getRoleId, roleId));
        for (Long permissionId : distinctIds) {
            RolePermission link = new RolePermission();
            link.setRoleId(roleId);
            link.setPermissionId(permissionId);
            rolePermissionMapper.insert(link);
        }
        audit(actor, "修改", role.getName());
    }

    @Override
    public List<RoleOptionVO> options() {
        return roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                        .ne(Role::getCode, SUPER_ADMIN_CODE)
                        .orderByAsc(Role::getId))
                .stream()
                .map(role -> new RoleOptionVO(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    private List<String> buildPermissionLabels(Role role, Set<Long> granted,
                                               List<Permission> permissions,
                                               Map<Long, Permission> permissionById) {
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            return List.of("全部权限");
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Permission permission : permissions) {
            if (permission.getType() == null || permission.getType() != 2
                    || !granted.contains(permission.getId())) {
                continue;
            }
            Permission group = permission.getParentId() == null
                    ? null
                    : permissionById.get(permission.getParentId());
            labels.add((group == null ? "" : group.getName()) + "·" + permission.getName());
        }
        if (labels.isEmpty()) {
            return List.of("暂无权限");
        }
        return new ArrayList<>(labels);
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void assertSuperAdmin() {
        AdminAccountView account = adminAccountService.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (!SUPER_ADMIN_CODE.equals(account.roleCode())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action, "角色",
                targetName, actor.ip());
    }
}
