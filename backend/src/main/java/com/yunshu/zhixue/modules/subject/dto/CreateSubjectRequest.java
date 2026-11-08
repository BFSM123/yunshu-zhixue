package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增学科表单。
 */
public record CreateSubjectRequest(
        @NotBlank(message = "请输入学科名称")
        @Size(max = 50, message = "学科名称不能超过 50 位")
        String name,

        @NotBlank(message = "请输入学科图标")
        @Size(max = 50, message = "学科图标不能超过 50 位")
        String icon,

        @NotBlank(message = "请输入学科描述")
        @Size(max = 255, message = "学科描述不能超过 255 位")
        String description,

        @Min(value = 0, message = "排序号不能小于 0")
        @Max(value = 9999, message = "排序号不能大于 9999")
        Integer sortOrder,

        @NotBlank(message = "请选择主题颜色")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "主题颜色格式应为 #RRGGBB")
        String themeColor
) {
}
