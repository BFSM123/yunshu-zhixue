package com.yunshu.zhixue.modules.student.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员 AI 面试记录（A-U07）。
 */
public record StudentInterviewVO(
        Long id,
        Long subjectId,
        String subjectName,
        Integer difficulty,
        Integer questionCount,
        BigDecimal overallScore,
        Integer totalDuration,
        LocalDateTime completedAt
) {
}