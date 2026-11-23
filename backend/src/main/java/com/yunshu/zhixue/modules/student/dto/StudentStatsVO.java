package com.yunshu.zhixue.modules.student.dto;

/**
 * 学员学习统计（口径与个人中心 P-M01 一致）。
 *
 * <p>学习天数为累计去重天数（按 Asia/Shanghai 自然日）；正确率按刷题明细聚合，无数据为 0。</p>
 */
public record StudentStatsVO(
        long studyDays,
        long completedCourseCount,
        long practiceCount,
        double accuracyRate
) {
}