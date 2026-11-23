package com.yunshu.zhixue.modules.route.dto;

/**
 * 路线详情中的课程项。
 *
 * @param otherSubject 课程不属于路线学科时为 true（跨学科补充课程）
 */
public record RouteCourseVO(
        Long routeCourseId,
        Long courseId,
        String name,
        String bvid,
        Integer durationMinutes,
        Integer source,
        String coverUrl,
        Long subjectId,
        String subjectName,
        Integer sortOrder,
        boolean otherSubject
) {
}
