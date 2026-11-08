package com.yunshu.zhixue.modules.system.dto;

/**
 * 重置密码结果：仅本次响应返回明文默认密码。
 */
public record ResetPasswordResult(String password) {
}
