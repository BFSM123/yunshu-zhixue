package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 上移/下移（A-S05 / A-S09）。
 */
public record SortRequest(
        @NotNull(message = "缺少排序对象")
        Long id,

        @NotBlank(message = "请指定移动方向")
        @Pattern(regexp = "^(up|down)$", message = "移动方向仅支持 up 或 down")
        String move
) {
}
