package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.CreateUserRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResult;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.dto.UpdateUserRequest;
import com.yunshu.zhixue.modules.system.dto.UserDetailVO;
import com.yunshu.zhixue.modules.system.dto.UserVO;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final String TARGET_TYPE = "后台用户";

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password}")
    private String defaultPassword;

    @Override
    public PageResult<UserVO> page(String keyword, Long roleId, Integer status, long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<AdminUser> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AdminUser::getUsername, kw)
                    .or()
                    .like(AdminUser::getMobile, kw));
        }
        if (roleId != null) {
            wrapper.eq(AdminUser::getRoleId, roleId);
        }
        if (status != null) {
            wrapper.eq(AdminUser::getStatus, status);
        }
        wrapper.orderByDesc(AdminUser::getCreatedAt).orderByAsc(AdminUser::getId);

        Page<AdminUser> result = adminUserMapper.selectPage(new Page<>(pageNo, size), wrapper);
        Map<Long, Role> roles = loadRoles(result.getRecords());
        List<UserVO> list = result.getRecords().stream()
                .map(user -> toVO(user, roles.get(user.getRoleId())))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public UserDetailVO detail(Long id) {
        AdminUser user = requireUser(id);
        Role role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        return new UserDetailVO(toVO(user, role), operationLogService.recentByUser(id, 10));
    }

    @Override
    public UserVO create(CreateUserRequest request, Actor actor) {
        String username = request.username().trim();
        String mobile = request.mobile().trim();
        Role role = requireRole(request.roleId());
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "不允许创建超级管理员账号");
        }
        assertUnique(username, mobile, null);

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setMobile(mobile);
        user.setEmail(normalizeEmail(request.email()));
        user.setRoleId(role.getId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(statusOrDefault(request.status()));
        user.setIsBuiltin(0);
        adminUserMapper.insert(user);

        audit(actor, "新增", username);
        return toVO(user, role);
    }

    @Override
    public UserVO update(Long id, UpdateUserRequest request, Actor actor) {
        AdminUser user = requireUser(id);
        String username = request.username().trim();
        String mobile = request.mobile().trim();
        Role role = requireRole(request.roleId());
        boolean builtin = user.getIsBuiltin() != null && user.getIsBuiltin() == 1;

        if (builtin && !role.getId().equals(user.getRoleId())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号角色不可修改");
        }
        if (!builtin && SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "不允许切换为超级管理员角色");
        }
        assertUnique(username, mobile, id);

        int status = statusOrDefault(request.status());
        String newPassword = request.newPassword();
        boolean passwordChanged = newPassword != null && !newPassword.isBlank();
        boolean roleChanged = !role.getId().equals(user.getRoleId());

        var update = Wrappers.lambdaUpdate(AdminUser.class)
                .eq(AdminUser::getId, id)
                .set(AdminUser::getUsername, username)
                .set(AdminUser::getMobile, mobile)
                .set(AdminUser::getEmail, normalizeEmail(request.email()))
                .set(AdminUser::getRoleId, role.getId())
                .set(AdminUser::getStatus, status);
        if (passwordChanged) {
            update.set(AdminUser::getPasswordHash, passwordEncoder.encode(newPassword));
        }
        adminUserMapper.update(null, update);

        if (roleChanged || status == 0 || passwordChanged) {
            tokenStore.bumpVersion(id);
        }

        user.setUsername(username);
        user.setMobile(mobile);
        user.setEmail(normalizeEmail(request.email()));
        user.setRoleId(role.getId());
        user.setStatus(status);
        audit(actor, "修改", username);
        return toVO(user, role);
    }

    @Override
    @Transactional
    public void batchStatus(List<Long> ids, int status, Actor actor) {
        int normalized = statusOrDefault(status);
        List<AdminUser> users = requireUsers(ids);
        adminUserMapper.update(null, Wrappers.lambdaUpdate(AdminUser.class)
                .in(AdminUser::getId, ids)
                .set(AdminUser::getStatus, normalized));
        users.forEach(user -> {
            if (normalized == 0) {
                tokenStore.bumpVersion(user.getId());
            }
            audit(actor, "禁用启用", user.getUsername());
        });
    }

    @Override
    public void delete(Long id, Actor actor) {
        AdminUser user = requireUser(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号不可删除");
        }
        adminUserMapper.deleteById(id);
        tokenStore.bumpVersion(id);
        audit(actor, "删除", user.getUsername());
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids, Actor actor) {
        List<AdminUser> users = requireUsers(ids);
        if (users.stream().anyMatch(user -> user.getIsBuiltin() != null && user.getIsBuiltin() == 1)) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号不可删除");
        }
        users.forEach(user -> {
            adminUserMapper.deleteById(user.getId());
            tokenStore.bumpVersion(user.getId());
            audit(actor, "删除", user.getUsername());
        });
    }

    @Override
    public ResetPasswordResult resetPassword(Long id, Actor actor) {
        AdminUser user = requireUser(id);
        adminUserMapper.update(null, Wrappers.lambdaUpdate(AdminUser.class)
                .eq(AdminUser::getId, id)
                .set(AdminUser::getPasswordHash, passwordEncoder.encode(defaultPassword)));
        tokenStore.bumpVersion(id);
        audit(actor, "修改", user.getUsername());
        return new ResetPasswordResult(defaultPassword);
    }

    @Override
    public List<RoleOptionVO> roleOptions() {
        return roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                        .ne(Role::getCode, SUPER_ADMIN_CODE)
                        .orderByAsc(Role::getId))
                .stream()
                .map(role -> new RoleOptionVO(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    private AdminUser requireUser(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<AdminUser> requireUsers(List<Long> ids) {
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<AdminUser> users = adminUserMapper.selectBatchIds(distinctIds);
        if (users.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return users;
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void assertUnique(String username, String mobile, Long excludeId) {
        LambdaQueryWrapper<AdminUser> usernameQuery =
                Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getUsername, username);
        if (excludeId != null) {
            usernameQuery.ne(AdminUser::getId, excludeId);
        }
        if (adminUserMapper.selectCount(usernameQuery) > 0) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "用户名已存在");
        }

        LambdaQueryWrapper<AdminUser> mobileQuery =
                Wrappers.lambdaQuery(AdminUser.class).eq(AdminUser::getMobile, mobile);
        if (excludeId != null) {
            mobileQuery.ne(AdminUser::getId, excludeId);
        }
        if (adminUserMapper.selectCount(mobileQuery) > 0) {
            throw new BizException(ErrorCode.MOBILE_DUPLICATED, "手机号已存在");
        }
    }

    private int statusOrDefault(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "状态不合法");
        }
        return status;
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
    }

    private Map<Long, Role> loadRoles(List<AdminUser> users) {
        Set<Long> roleIds = users.stream()
                .map(AdminUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
    }

    private UserVO toVO(AdminUser user, Role role) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getMobile(),
                user.getEmail(),
                user.getRoleId(),
                role == null ? null : role.getName(),
                role == null ? null : role.getCode(),
                user.getStatus(),
                user.getIsBuiltin() != null && user.getIsBuiltin() == 1,
                TimeUtils.toShanghai(user.getLastLoginAt()),
                TimeUtils.toShanghai(user.getCreatedAt())
        );
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action, TARGET_TYPE,
                targetName, actor.ip());
    }
}
