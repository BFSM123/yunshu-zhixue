package com.yunshu.zhixue.modules.question.dto;

/**
 * 导入结果（A-Q13）。
 */
public record QuestionImportResultVO(
        long importedCount,
        int skippedCount,
        int createdSubjectCount,
        int createdStageCount
) {
}
