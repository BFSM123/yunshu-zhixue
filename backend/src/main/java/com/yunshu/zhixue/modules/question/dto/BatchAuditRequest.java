package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量审核（导入后一次处理多条待审核题目，属 A-Q07 的批量补充）。
 */
public record BatchAuditRequest(

        @NotEmpty(message = "请选择题目")
        @Size(max = 500, message = "单次最多操作 500 道题目")
        List<Long> ids,

        @NotNull(message = "请选择审核结果")
        Boolean pass,

        @Size(max = 255, message = "驳回原因不能超过 255 字")
        String rejectReason
) {
}
