package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * 学习路线卡片（A-R01）。
 */
public record RouteCardVO(
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
        List<RouteCoursePreviewVO> previewCourses
) {
}
