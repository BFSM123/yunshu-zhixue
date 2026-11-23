package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量删除学员（A-U05）。
 */
public record StudentBatchIdsRequest(
        @NotEmpty(message = "请选择学员")
        List<Long> ids
) {
}