package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 单题上架/下架（A-Q08）。action：on 上架 / off 下架。
 */
public record QuestionStatusRequest(

        @NotBlank(message = "请指定操作")
        @Pattern(regexp = "on|off", message = "操作只能是 on 或 off")
        String action
) {
}
