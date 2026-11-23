package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 面试题列表项（A-I01）。
 */
public record InterviewQuestionListItemVO(
        Long id,
        String content,
        Long subjectId,
        String subjectName,
        String topic,
        Integer difficulty,
        Integer status,
        String tags,
        Integer sourceType,
        String sourceDocName,
        Integer sourceSeq
) {
}