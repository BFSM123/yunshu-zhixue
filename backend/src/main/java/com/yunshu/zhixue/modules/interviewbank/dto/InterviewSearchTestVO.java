package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * 检索测试结果（A-I09）。
 */
public record InterviewSearchTestVO(
        String keyword,
        long total,
        List<Item> items
) {

    public record Item(
            Long id,
            String content,
            Long subjectId,
            String subjectName,
            String topic,
            Integer difficulty,
            Integer sourceType,
            String sourceDocName,
            Integer sourceSeq
    ) {
    }
}