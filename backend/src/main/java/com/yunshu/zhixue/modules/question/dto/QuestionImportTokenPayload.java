package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * 导入预览在 Redis 中的暂存内容：逐行解析结果。
 * 学科与阶段已随行解析，确认入库时按行创建缺失的学科与阶段。
 */
public record QuestionImportTokenPayload(
        List<QuestionImportPreviewVO.Row> rows
) {
}
