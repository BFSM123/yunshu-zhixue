package com.yunshu.zhixue.common.util;

/**
 * 敏感信息脱敏工具。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /** 手机号脱敏：138****1234。 */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
