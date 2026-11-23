package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Excel 导入预览暂存内容（Redis，TTL 30 分钟）。
 */
public record InterviewImportTokenPayload(
        String sourceDocName,
        List<InterviewImportPreviewVO.Row> rows
) {
}