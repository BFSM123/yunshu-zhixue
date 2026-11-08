package com.yunshu.zhixue.common.response;

/**
 * 参数校验失败的字段级错误（api-rules：校验错误需指明字段与原因）。
 */
public record FieldErrorItem(String field, String reason) {
}
