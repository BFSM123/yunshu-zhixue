package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.AdminAccountView;

import java.util.Optional;

/**
 * 后台账号读取服务：供认证模块查询账号、角色与权限。
 */
public interface AdminAccountService {

    Optional<AdminAccountView> findByAccount(String account);

    Optional<AdminAccountView> findById(Long id);

    void recordLogin(Long id);
}
