package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 学员/后台账号状态：1 已启用 / 0 已禁用。 */
@Getter
@RequiredArgsConstructor
public enum AccountStatus {

    DISABLED(0, "已禁用"),
    ENABLED(1, "已启用");

    private final int code;
    private final String label;
}
