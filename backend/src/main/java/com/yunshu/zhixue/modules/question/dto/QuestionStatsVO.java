package com.yunshu.zhixue.modules.question.dto;

/**
 * 题库统计卡（A-Q01）。
 */
public record QuestionStatsVO(
        long total,
        long onShelf,
        long pending,
        long single,
        long multiple,
        long judgment,
        long shortAnswer
) {
}
