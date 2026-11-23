package com.yunshu.zhixue.modules.question.dto;

/**
 * 题目选项视图。
 */
public record QuestionOptionVO(
        Long id,
        String optionKey,
        String optionText,
        Integer isCorrect,
        Integer sortOrder
) {
}
