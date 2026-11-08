package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.SettingsVO;
import com.yunshu.zhixue.modules.system.dto.UpdateSettingsRequest;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import com.yunshu.zhixue.modules.system.mapper.SystemSettingMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_SITE_DESCRIPTION = "site_description";
    private static final String KEY_LOGO_URL = "logo_url";
    private static final String KEY_DEFAULT_AVATAR = "default_avatar";

    private static final String DEFAULT_SITE_NAME = "云枢智学";
    private static final String DEFAULT_SITE_DESCRIPTION = "AI 赋能的智能面试训练平台";

    private static final String TARGET_TYPE = "系统设置";

    private final SystemSettingMapper systemSettingMapper;
    private final OperationLogService operationLogService;

    @Override
    public SettingsVO get() {
        Map<String, String> values = systemSettingMapper.selectList(null).stream()
                .collect(Collectors.toMap(SystemSetting::getConfigKey,
                        setting -> setting.getConfigValue() == null ? "" : setting.getConfigValue(),
                        (first, second) -> second));
        return new SettingsVO(
                values.getOrDefault(KEY_SITE_NAME, DEFAULT_SITE_NAME),
                values.getOrDefault(KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION),
                values.getOrDefault(KEY_LOGO_URL, ""),
                values.getOrDefault(KEY_DEFAULT_AVATAR, "")
        );
    }

    @Override
    @Transactional
    public void update(UpdateSettingsRequest request, Actor actor) {
        upsert(KEY_SITE_NAME, request.siteName().trim(), "平台名称");
        upsert(KEY_SITE_DESCRIPTION, normalize(request.siteDescription()), "平台简介");
        upsert(KEY_LOGO_URL, normalize(request.logoUrl()), "平台 Logo 地址");
        upsert(KEY_DEFAULT_AVATAR, normalize(request.defaultAvatar()), "默认头像地址");
        audit(actor, "基本信息");
    }

    @Override
    @Transactional
    public void reset(Actor actor) {
        upsert(KEY_SITE_NAME, DEFAULT_SITE_NAME, "平台名称");
        upsert(KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION, "平台简介");
        upsert(KEY_LOGO_URL, "", "平台 Logo 地址");
        upsert(KEY_DEFAULT_AVATAR, "", "默认头像地址");
        audit(actor, "恢复默认设置");
    }

    private void upsert(String key, String value, String remark) {
        SystemSetting existing = systemSettingMapper.selectOne(
                Wrappers.lambdaQuery(SystemSetting.class).eq(SystemSetting::getConfigKey, key));
        if (existing == null) {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setRemark(remark);
            systemSettingMapper.insert(setting);
        } else {
            systemSettingMapper.update(null, Wrappers.lambdaUpdate(SystemSetting.class)
                    .eq(SystemSetting::getId, existing.getId())
                    .set(SystemSetting::getConfigValue, value));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void audit(Actor actor, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), "修改", TARGET_TYPE,
                targetName, actor.ip());
    }
}
