package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增阶段表单。
 */
public record CreateStageRequest(
        @NotBlank(message = "请输入阶段名称")
        @Size(max = 50, message = "阶段名称不能超过 50 位")
        String name,

        @Size(max = 50, message = "阶段图标不能超过 50 位")
        String icon,

        @Min(value = 0, message = "排序号不能小于 0")
        @Max(value = 9999, message = "排序号不能大于 9999")
        Integer sortOrder
) {
}
