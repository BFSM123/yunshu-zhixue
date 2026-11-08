package com.yunshu.zhixue.modules.system.dto;

/**
 * 当前操作人（用于审计）。
 */
public record Actor(Long userId, String username, String ip) {
}
