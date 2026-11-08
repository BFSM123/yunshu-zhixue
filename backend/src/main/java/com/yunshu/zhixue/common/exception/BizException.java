package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.response.FieldErrorItem;
import com.yunshu.zhixue.common.response.FileErrorItem;
import lombok.Getter;

import java.util.List;

/**
 * 业务异常：由全局异常处理器统一转换为 HTTP 状态码 + 业务错误码响应体。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorItem> fieldErrors;
    private final List<FileErrorItem> fileErrors;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = null;
        this.fileErrors = null;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = null;
        this.fileErrors = null;
    }

    public BizException(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
        this.fileErrors = null;
    }

    public BizException(ErrorCode errorCode, String message,
                        List<FieldErrorItem> fieldErrors, List<FileErrorItem> fileErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
        this.fileErrors = fileErrors;
    }
}
