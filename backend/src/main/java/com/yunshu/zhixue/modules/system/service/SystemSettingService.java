package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.dto.SettingsVO;
import com.yunshu.zhixue.modules.system.dto.UpdateSettingsRequest;

/**
 * 系统基本信息设置。
 */
public interface SystemSettingService {

    SettingsVO get();

    void update(UpdateSettingsRequest request, Actor actor);

    void reset(Actor actor);
}
