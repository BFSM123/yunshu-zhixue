package com.yunshu.zhixue.modules.question.dto;

import java.time.LocalDateTime;

/**
 * 题库列表项（A-Q02）。
 */
public record QuestionListItemVO(
        Long id,
        String content,
        Long stageId,
        String stageName,
        Long subjectId,
        String subjectName,
        Integer type,
        Integer difficulty,
        Integer status,
        String tags,
        LocalDateTime createdAt
) {
}
