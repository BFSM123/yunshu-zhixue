package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 学习路线状态：1 启用 / 0 禁用。 */
@Getter
@RequiredArgsConstructor
public enum RouteStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String label;
}
