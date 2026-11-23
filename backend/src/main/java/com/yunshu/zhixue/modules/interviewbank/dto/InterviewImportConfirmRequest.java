package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 导入确认（A-I08）。
 */
public record InterviewImportConfirmRequest(

        @NotBlank(message = "导入凭证不能为空")
        @Size(max = 64, message = "导入凭证不合法")
        String token
) {
}