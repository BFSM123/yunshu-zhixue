package com.yunshu.zhixue.modules.interviewbank.dto;

import java.time.LocalDateTime;

/**
 * 面试题详情（A-I02）。
 */
public record InterviewQuestionDetailVO(
        Long id,
        String content,
        Long subjectId,
        String subjectName,
        String referenceAnswer,
        String topic,
        Integer difficulty,
        Integer status,
        String tags,
        Integer sourceType,
        String sourceDocName,
        Integer sourceSeq,
        long referencedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}