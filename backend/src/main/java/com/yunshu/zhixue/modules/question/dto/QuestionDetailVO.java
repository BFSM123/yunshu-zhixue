package com.yunshu.zhixue.modules.question.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情（A-Q03）。answer 为组装字段：
 * 单选/多选返回正确项标识（如 A、A,B,C），判断/简答返回原文答案。
 */
public record QuestionDetailVO(
        Long id,
        String content,
        Long stageId,
        String stageName,
        Long subjectId,
        String subjectName,
        Integer type,
        Integer difficulty,
        Integer status,
        String rejectReason,
        String tags,
        String answer,
        String analysis,
        List<QuestionOptionVO> options,
        long doneCount,
        long correctCount,
        BigDecimal accuracyRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
