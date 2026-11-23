package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 单题审核（A-Q07）。
 */
public record QuestionAuditRequest(

        @NotNull(message = "请选择审核结果")
        Boolean pass,

        @Size(max = 255, message = "驳回原因不能超过 255 字")
        String rejectReason
) {
}
