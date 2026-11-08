package com.yunshu.zhixue.modules.system.dto;

/**
 * 系统基本信息设置。
 */
public record SettingsVO(
        String siteName,
        String siteDescription,
        String logoUrl,
        String defaultAvatar
) {
}
