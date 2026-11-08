package com.yunshu.zhixue.modules.subject.dto;

import java.util.List;

/**
 * 学科卡片（A-S02）。课程数按主学科 + 关联学科去重统计。
 */
public record SubjectCardVO(
        Long id,
        String name,
        String icon,
        String description,
        String themeColor,
        Integer sortOrder,
        long courseCount,
        long routeCount,
        long questionCount,
        List<StageVO> stages
) {
}
