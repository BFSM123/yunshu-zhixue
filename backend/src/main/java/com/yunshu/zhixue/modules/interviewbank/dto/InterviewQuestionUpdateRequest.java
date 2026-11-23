package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 编辑面试题（A-I04）：仅允许修改主题与难度，题干与参考答案保持与来源一致。
 */
public record InterviewQuestionUpdateRequest(

        @Size(max = 50, message = "主题不能超过 50 字")
        String topic,

        @NotNull(message = "请选择难度")
        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty
) {
}