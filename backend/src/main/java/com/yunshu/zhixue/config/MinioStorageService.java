package com.yunshu.zhixue.config;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 实现。bucket 不存在时首次上传自动创建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String upload(InputStream inputStream, long size, String contentType, String objectName) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return publicUrl(objectName);
        } catch (Exception ex) {
            log.error("对象上传失败, objectName={}", objectName, ex);
            throw new BizException(ErrorCode.FILE_INVALID, "文件上传失败");
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            log.error("对象删除失败, objectName={}", objectName, ex);
        }
    }

    @Override
    public String publicUrl(String objectName) {
        String base = properties.getPublicUrl();
        if (base == null || base.isBlank()) {
            base = properties.getEndpoint();
        }
        return trimTrailingSlash(base) + "/" + properties.getBucket() + "/" + objectName;
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
