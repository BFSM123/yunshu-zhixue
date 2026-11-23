package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新建学习路线（A-R02）。
 */
public record CreateRouteRequest(
        @NotBlank(message = "请输入路线名称")
        @Size(max = 100, message = "路线名称不能超过 100 位")
        String name,

        @NotNull(message = "请选择关联学科")
        Long subjectId,

        @Min(value = 0, message = "路线状态不合法")
        @Max(value = 1, message = "路线状态不合法")
        Integer status,

        @Size(max = 50, message = "路线图标不能超过 50 位")
        String icon,

        @Size(max = 100, message = "适合学历不能超过 100 位")
        String targetEducation,

        @Size(max = 2000, message = "路线描述不能超过 2000 位")
        String description
) {
}
