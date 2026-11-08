package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * 课程列表项（A-C02）。subjectIds / subjectNames 为额外关联学科；
 * subjectId 为空时 subjectName 固定返回「其他学科」。
 */
public record CourseVO(
        Long id,
        String name,
        String coverUrl,
        Integer durationMinutes,
        Integer source,
        String link,
        String bvid,
        Long subjectId,
        String subjectName,
        List<Long> subjectIds,
        List<String> subjectNames
) {
}
