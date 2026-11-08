package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 操作日志视图。
 */
public record OperationLogVO(
        Long id,
        Long userId,
        String username,
        String action,
        String targetType,
        String targetName,
        String ip,
        LocalDateTime createdAt
) {
}
