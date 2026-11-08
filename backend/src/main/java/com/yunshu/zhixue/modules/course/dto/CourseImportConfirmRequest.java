package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 批量导入确认请求（A-C07）：携带预览返回的一次性 token。
 */
public record CourseImportConfirmRequest(
        @NotBlank(message = "导入 token 不能为空")
        String token
) {
}
