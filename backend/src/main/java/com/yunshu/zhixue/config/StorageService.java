package com.yunshu.zhixue.config;

import java.io.InputStream;

/**
 * 对象存储抽象（S3 兼容）：业务只依赖本接口，实现可切换 MinIO / 云 OSS / COS。
 */
public interface StorageService {

    /**
     * 上传对象并返回可访问 URL。
     *
     * @param inputStream 文件流
     * @param size        文件字节数，未知时传 -1
     * @param contentType MIME 类型
     * @param objectName  对象名（由服务端生成 UUID，不接受用户可控路径）
     */
    String upload(InputStream inputStream, long size, String contentType, String objectName);

    void delete(String objectName);

    String publicUrl(String objectName);
}
