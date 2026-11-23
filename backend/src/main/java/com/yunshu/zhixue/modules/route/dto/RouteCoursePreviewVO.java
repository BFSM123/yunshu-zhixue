package com.yunshu.zhixue.modules.route.dto;

/**
 * 路线卡片中的课程预览项（前 4 门）。
 */
public record RouteCoursePreviewVO(
        Long routeCourseId,
        Long courseId,
        String name,
        String bvid,
        Integer durationMinutes
) {
}
