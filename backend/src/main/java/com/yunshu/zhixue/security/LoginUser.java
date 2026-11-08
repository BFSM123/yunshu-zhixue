package com.yunshu.zhixue.security;

import java.util.Set;

/**
 * 当前登录主体（学员或后台用户）。
 *
 * @param id          用户主键
 * @param audience    所属端
 * @param permissions 管理端权限码集合；学员端为空集合
 */
public record LoginUser(Long id, TokenAudience audience, Set<String> permissions) {

    public boolean hasPermission(String code) {
        return permissions != null && permissions.contains(code);
    }
}
