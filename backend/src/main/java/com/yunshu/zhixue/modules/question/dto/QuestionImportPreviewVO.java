package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * Excel 导入解析结果（A-Q12）。
 * status：valid 可导入 / warning 可导入但需留意 / invalid 不可导入。
 * duplicate=true 表示与库中或本文件前面的题目完全重复，确认导入时自动跳过。
 * 学科与阶段均由文件内容确定，不存在时导入自动创建。
 */
public record QuestionImportPreviewVO(
        String token,
        int totalRows,
        int validCount,
        int warningCount,
        int invalidCount,
        // 完全重复、确认导入时跳过的行数。
        int duplicateCount,
        int autoCreateSubjectCount,
        int autoCreateStageCount,
        List<Row> rows
) {

    /**
     * 单行解析结果。选项 key 保留 Excel 原始标识（A~F）。
     */
    public record Row(
            int row,
            String content,
            String subjectName,
            Long subjectId,
            boolean subjectAutoCreate,
            String stageName,
            Long stageId,
            boolean stageAutoCreate,
            Integer type,
            Integer difficulty,
            String answer,
            String analysis,
            String tags,
            List<OptionPreview> options,
            // 完全重复（库中已存在或文件内重复），确认导入时跳过。
            boolean duplicate,
            String status,
            List<String> messages
    ) {
    }

    public record OptionPreview(
            String optionKey,
            String optionText,
            boolean correct
    ) {
    }
}
