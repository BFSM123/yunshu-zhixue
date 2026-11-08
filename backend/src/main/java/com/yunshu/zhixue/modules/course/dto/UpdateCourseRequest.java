package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 编辑课程表单（A-C04），字段约束与新增一致。
 */
public record UpdateCourseRequest(
        @NotBlank(message = "请输入课程名称")
        @Size(max = 100, message = "课程名称不能超过 100 位")
        String name,

        @Size(max = 500, message = "封面 URL 不能超过 500 位")
        @Pattern(regexp = "^$|^https?://.+", message = "封面 URL 需以 http:// 或 https:// 开头")
        String coverUrl,

        @NotNull(message = "请输入课程时长")
        @Min(value = 1, message = "课程时长必须大于 0 分钟")
        @Max(value = 100_000, message = "课程时长不能超过 100000 分钟")
        Integer durationMinutes,

        @NotNull(message = "请选择课程来源")
        @Min(value = 1, message = "课程来源不合法")
        @Max(value = 2, message = "课程来源不合法")
        Integer source,

        @Size(max = 500, message = "课程链接不能超过 500 位")
        @Pattern(regexp = "^$|^https?://.+", message = "课程链接需以 http:// 或 https:// 开头")
        String link,

        Long subjectId,

        List<Long> subjectIds
) {
}
