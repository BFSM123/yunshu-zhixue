package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * 学员详情（A-U06）：资料区 + 学习统计 + 累计面试次数。
 */
public record StudentDetailVO(
        Long id,
        String nickname,
        String avatar,
        String mobile,
        String email,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        StudentStatsVO stats,
        long interviewCount
) {
}