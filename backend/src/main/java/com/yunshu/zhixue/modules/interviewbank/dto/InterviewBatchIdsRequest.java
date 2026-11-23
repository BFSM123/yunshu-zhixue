package com.yunshu.zhixue.modules.interviewbank.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量删除面试题。
 */
public record InterviewBatchIdsRequest(

        @NotEmpty(message = "请选择面试题")
        @Size(max = 500, message = "单次最多操作 500 道面试题")
        List<Long> ids
) {
}