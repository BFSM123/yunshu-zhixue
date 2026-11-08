package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
            SELECT p.* FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId} AND p.delete_flag = 0
            ORDER BY p.sort_order, p.id
            """)
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);
}
