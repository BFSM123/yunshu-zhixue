package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 路线启用/禁用（A-R04）。
 */
public record UpdateRouteStatusRequest(
        @NotNull(message = "请选择路线状态")
        @Min(value = 0, message = "路线状态不合法")
        @Max(value = 1, message = "路线状态不合法")
        Integer status
) {
}
