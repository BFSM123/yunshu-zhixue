package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 权限节点类型：1 组（菜单级）/ 2 操作点。 */
@Getter
@RequiredArgsConstructor
public enum PermissionType {

    GROUP(1, "组"),
    ACTION(2, "操作点");

    private final int code;
    private final String label;
}
