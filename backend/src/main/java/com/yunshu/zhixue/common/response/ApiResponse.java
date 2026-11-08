package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一响应结构（04 文档 D4-1 方案 A）。
 *
 * <p>成功：HTTP 2xx + {@code {code:0, message:"ok", data}}；<br>
 * 失败：真实 HTTP 状态码 + {@code {code:业务码, message, data:null, fieldErrors?}}。</p>
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        List<FieldErrorItem> fieldErrors,
        List<FileErrorItem> fileErrors
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, List<FieldErrorItem> fieldErrors) {
        return new ApiResponse<>(code, message, null, fieldErrors, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message,
                                          List<FieldErrorItem> fieldErrors,
                                          List<FileErrorItem> fileErrors) {
        return new ApiResponse<>(code, message, null, fieldErrors, fileErrors);
    }
}
