package com.yunshu.zhixue.security;

/**
 * 令牌端标识：两端独立 token 域，互不通用（D12）。
 */
public enum TokenAudience {

    PORTAL("portal", "/api/portal"),
    ADMIN("admin", "/api/admin");

    private final String value;
    private final String pathPrefix;

    TokenAudience(String value, String pathPrefix) {
        this.value = value;
        this.pathPrefix = pathPrefix;
    }

    public String value() {
        return value;
    }

    public String pathPrefix() {
        return pathPrefix;
    }

    public static TokenAudience fromRequestPath(String uri) {
        return uri.startsWith(ADMIN.pathPrefix) ? ADMIN : PORTAL;
    }
}
