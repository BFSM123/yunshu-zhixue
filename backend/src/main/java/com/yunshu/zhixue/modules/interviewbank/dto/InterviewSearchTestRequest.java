package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 检索测试（A-I09）：按关键词验证题库内容，不依赖 AI。
 */
public record InterviewSearchTestRequest(

        @NotBlank(message = "请输入检索关键词")
        @Size(max = 100, message = "检索关键词不能超过 100 字")
        String keyword,

        Long subjectId,

        @Min(value = 1, message = "最多返回 50 条")
        @Max(value = 50, message = "最多返回 50 条")
        Integer limit
) {
}