package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Excel 导入解析结果（A-I07）。
 * status：valid 可导入 / warning 可导入但需留意 / invalid 不可导入。
 * duplicate=true 表示与库中或本文件前面的题目重复，确认导入时自动跳过。
 * 学科由文件内容确定，不存在时导入自动创建。
 */
public record InterviewImportPreviewVO(
        String token,
        String sourceDocName,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        int duplicateCount,
        int autoCreateSubjectCount,
        // 词表外主题种数（导入后可在主题管理中维护）。
        int newTopicCount,
        List<Row> rows
) {

    /**
     * 单行解析结果。
     */
    public record Row(
            int row,
            String content,
            String subjectName,
            Long subjectId,
            boolean subjectAutoCreate,
            String topic,
            boolean topicNew,
            Integer difficulty,
            String tags,
            String referenceAnswer,
            boolean duplicate,
            String status,
            List<String> messages
    ) {
    }
}