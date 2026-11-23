package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 导入结果（A-I08）。
 */
public record InterviewImportResultVO(
        long importedCount,
        int skippedCount,
        int createdSubjectCount
) {
}