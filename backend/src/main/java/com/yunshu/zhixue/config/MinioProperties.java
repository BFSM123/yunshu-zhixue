package com.yunshu.zhixue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO / S3 兼容对象存储配置。
 */
@Data
@ConfigurationProperties(prefix = "app.storage")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket = "yszx";
    private String publicUrl;
}
