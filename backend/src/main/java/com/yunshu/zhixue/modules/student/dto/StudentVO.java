package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * 学员列表行（A-U01）：资料 + 学习天数 / 刷题·面试数。
 */
public record StudentVO(
        Long id,
        String nickname,
        String avatar,
        String mobile,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        long studyDays,
        long practiceCount,
        long interviewCount
) {
}