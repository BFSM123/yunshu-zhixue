package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 路线课程排序（A-R09），按 routeCourseIds 顺序重写 sort_order。
 */
public record SortRouteCoursesRequest(
        @NotEmpty(message = "排序列表不能为空")
        @Size(max = 500, message = "排序列表超出课程上限")
        List<Long> routeCourseIds
) {
}
