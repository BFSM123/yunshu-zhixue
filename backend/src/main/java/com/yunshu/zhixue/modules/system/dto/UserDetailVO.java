package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 用户详情：资料 + 最近操作。
 */
public record UserDetailVO(UserVO user, List<OperationLogVO> recentOperations) {
}
