package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * 学习路线详情（A-R06）。
 */
public record RouteDetailVO(
        Long id,
        String name,
        Long subjectId,
        String subjectName,
        Integer status,
        String icon,
        String targetEducation,
        String description,
        long courseCount,
        long totalDurationMinutes,
        List<RouteCourseVO> courses
) {
}
