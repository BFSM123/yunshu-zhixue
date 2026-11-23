package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增/编辑题目请求（A-Q04 / A-Q05）。
 * 单选/多选使用 options；判断/简答使用 answer。
 */
public record QuestionSaveRequest(

        @NotBlank(message = "题干不能为空")
        @Size(max = 5000, message = "题干不能超过 5000 字")
        String content,

        @NotNull(message = "请选择阶段")
        Long stageId,

        @NotNull(message = "请选择题型")
        @Min(value = 1, message = "题型不合法")
        @Max(value = 4, message = "题型不合法")
        Integer type,

        @NotNull(message = "请选择难度")
        @Min(value = 1, message = "难度不合法")
        @Max(value = 3, message = "难度不合法")
        Integer difficulty,

        @Size(max = 255, message = "标签不能超过 255 字")
        String tags,

        @Size(max = 5000, message = "解析不能超过 5000 字")
        String analysis,

        @Size(max = 5000, message = "答案不能超过 5000 字")
        String answer,

        List<@Valid OptionItem> options
) {

    /**
     * 选项提交项。正确标识由 correct 表达，选项标识由服务端按顺序生成 A/B/C...
     */
    public record OptionItem(

            @NotBlank(message = "选项内容不能为空")
            @Size(max = 500, message = "选项内容不能超过 500 字")
            String optionText,

            Boolean correct
    ) {
    }
}
