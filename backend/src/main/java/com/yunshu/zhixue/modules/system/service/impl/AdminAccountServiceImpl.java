package com.yunshu.zhixue.modules.system.service.impl;

import com.yunshu.zhixue.modules.system.dto.AdminAccountView;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private final AdminUserMapper adminUserMapper;

    @Override
    public Optional<AdminAccountView> findByAccount(String account) {
        if (account == null || account.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(adminUserMapper.selectByAccount(account.trim())).map(this::toView);
    }

    @Override
    public Optional<AdminAccountView> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adminUserMapper.selectById(id)).map(this::toView);
    }

    @Override
    public void recordLogin(Long id) {
        adminUserMapper.updateLastLogin(id, LocalDateTime.now(ZoneOffset.UTC));
    }

    private AdminAccountView toView(AdminUser user) {
        String roleCode = adminUserMapper.selectRoleCode(user.getRoleId());
        LinkedHashSet<String> permissions =
                new LinkedHashSet<>(adminUserMapper.selectPermissionCodes(user.getRoleId()));
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new AdminAccountView(user.getId(), user.getUsername(), user.getPasswordHash(),
                roleCode, enabled, permissions);
    }
}
