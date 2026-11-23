package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量上架/下架。action：on 上架 / off 下架。
 */
public record BatchInterviewStatusRequest(

        @NotEmpty(message = "请选择面试题")
        @Size(max = 500, message = "单次最多操作 500 道面试题")
        List<Long> ids,

        @NotBlank(message = "请指定操作")
        @Pattern(regexp = "on|off", message = "操作只能是 on 或 off")
        String action
) {
}