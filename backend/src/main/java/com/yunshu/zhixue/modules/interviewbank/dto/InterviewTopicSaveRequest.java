package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增/编辑主题（A-I11 / A-I12）。subjectId 为空表示全局主题。
 */
public record InterviewTopicSaveRequest(

        @NotBlank(message = "主题名称不能为空")
        @Size(max = 50, message = "主题名称不能超过 50 字")
        String name,

        Long subjectId,

        Integer sortOrder
) {
}