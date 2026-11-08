package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户查询：登录认证所需的账号、角色与权限读取。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /**
     * 按用户名或手机号查询账号；用户名优先命中（Q4 定稿）。
     */
    @Select("""
            SELECT * FROM admin_user
            WHERE delete_flag = 0 AND (username = #{account} OR mobile = #{account})
            ORDER BY (username = #{account}) DESC, id ASC
            LIMIT 1
            """)
    AdminUser selectByAccount(@Param("account") String account);

    @Select("SELECT code FROM role WHERE id = #{roleId} AND delete_flag = 0")
    String selectRoleCode(@Param("roleId") Long roleId);

    @Select("""
            SELECT p.code FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId} AND p.delete_flag = 0
            ORDER BY p.sort_order, p.id
            """)
    List<String> selectPermissionCodes(@Param("roleId") Long roleId);

    @Update("UPDATE admin_user SET last_login_at = #{time} WHERE id = #{id}")
    int updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);
}
