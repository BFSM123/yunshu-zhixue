package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 主题列表项（A-I10）。
 * inVocabulary=true 为词表主题；false 为题目中使用到但词表未维护的主题（id 为空）。
 */
public record InterviewTopicVO(
        Long id,
        String name,
        Long subjectId,
        String subjectName,
        Integer sortOrder,
        long questionCount,
        boolean inVocabulary
) {
}