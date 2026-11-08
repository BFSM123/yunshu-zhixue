package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 阶段启用/禁用（A-S10）。
 */
public record UpdateStageStatusRequest(
        @NotNull(message = "请指定阶段状态")
        @Min(value = 0, message = "状态仅支持 0 或 1")
        @Max(value = 1, message = "状态仅支持 0 或 1")
        Integer status
) {
}
