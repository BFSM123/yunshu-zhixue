package com.yunshu.zhixue.modules.course.dto;

import java.math.BigDecimal;

/**
 * 课程库统计卡（A-C01）：总课程数、总时长（小时，保留 1 位小数）、关联学科数。
 * 关联学科数按主学科 + 额外学科去重统计，「其他学科」（主学科为空）不计入。
 */
public record CourseStatsVO(
        long totalCourses,
        BigDecimal totalDurationHours,
        long relatedSubjectCount
) {
}
