package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.CreateRoleRequest;
import com.yunshu.zhixue.modules.system.dto.PermissionNodeVO;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.dto.RoleVO;

import java.util.List;

/**
 * 角色与权限管理（仅超级管理员，role/options 除外）。
 */
public interface RoleService {

    List<RoleVO> list();

    RoleVO create(CreateRoleRequest request, Actor actor);

    void delete(Long id, Actor actor);

    List<PermissionNodeVO> tree(Long roleId);

    void savePermissions(Long roleId, List<Long> permissionIds, Actor actor);

    List<RoleOptionVO> options();
}
