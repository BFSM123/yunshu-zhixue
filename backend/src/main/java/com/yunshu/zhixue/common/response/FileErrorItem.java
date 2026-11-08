package com.yunshu.zhixue.common.response;

/**
 * 批量导入的文件行级错误（业务码 30005 专用）。
 */
public record FileErrorItem(int row, String field, String reason) {
}
