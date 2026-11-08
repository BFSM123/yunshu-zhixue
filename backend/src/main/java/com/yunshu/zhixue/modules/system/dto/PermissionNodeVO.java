package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 权限树节点（组节点含 children）。
 */
public record PermissionNodeVO(
        Long id,
        String name,
        String code,
        boolean checked,
        List<PermissionNodeVO> children
) {
}
