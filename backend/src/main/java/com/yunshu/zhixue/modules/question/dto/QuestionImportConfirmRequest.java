package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 导入确认入库（A-Q13）。
 */
public record QuestionImportConfirmRequest(

        @NotBlank(message = "预览标识不能为空")
        String token
) {
}
