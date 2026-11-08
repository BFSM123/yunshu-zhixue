package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.CreateUserRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResult;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.dto.UpdateUserRequest;
import com.yunshu.zhixue.modules.system.dto.UserDetailVO;
import com.yunshu.zhixue.modules.system.dto.UserVO;

import java.util.List;

/**
 * 后台用户管理。
 */
public interface AdminUserService {

    PageResult<UserVO> page(String keyword, Long roleId, Integer status, long page, long pageSize);

    UserDetailVO detail(Long id);

    UserVO create(CreateUserRequest request, Actor actor);

    UserVO update(Long id, UpdateUserRequest request, Actor actor);

    void batchStatus(List<Long> ids, int status, Actor actor);

    void delete(Long id, Actor actor);

    void batchDelete(List<Long> ids, Actor actor);

    ResetPasswordResult resetPassword(Long id, Actor actor);

    List<RoleOptionVO> roleOptions();
}
