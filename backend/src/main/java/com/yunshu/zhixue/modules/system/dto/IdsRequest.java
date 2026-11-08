package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量主键集合。
 */
public record IdsRequest(
        @NotEmpty(message = "请选择记录")
        List<Long> ids
) {
}
