package com.yunshu.zhixue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 操作日志动作类型。DB {@code operation_log.action} 直接存中文值（附录 C）。
 */
@Getter
@RequiredArgsConstructor
public enum OperationAction {

    LOGIN("登录"),
    LOGOUT("登出"),
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    TOGGLE_STATUS("禁用启用"),
    EXPORT("导出"),
    OTHER("其他");

    private final String label;

    public static Optional<OperationAction> of(String label) {
        return Arrays.stream(values()).filter(v -> v.label.equals(label)).findFirst();
    }
}
