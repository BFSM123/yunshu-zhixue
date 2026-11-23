package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量删除（A-Q11）。
 */
public record BatchIdsRequest(

        @NotEmpty(message = "请选择题目")
        @Size(max = 500, message = "单次最多操作 500 道题目")
        List<Long> ids
) {
}
