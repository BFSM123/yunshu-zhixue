package com.yunshu.zhixue.modules.system.service.impl;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.config.StorageService;
import com.yunshu.zhixue.modules.system.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final int LOGO_SIZE = 256;
    private static final int AVATAR_SIZE = 200;

    private final StorageService storageService;

    @Override
    public String uploadLogo(MultipartFile file) {
        return upload(file, "logo", LOGO_SIZE, LOGO_SIZE);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        return upload(file, "avatar", AVATAR_SIZE, AVATAR_SIZE);
    }

    private String upload(MultipartFile file, String prefix, int maxWidth, int maxHeight) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "请选择图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片大小不能超过 5MB");
        }

        String contentType = file.getContentType();
        String extension;
        if ("image/png".equals(contentType)) {
            extension = "png";
        } else if ("image/jpeg".equals(contentType)) {
            extension = "jpg";
        } else {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG / PNG 图片");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BizException(ErrorCode.FILE_INVALID, "图片内容无法识别");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > maxWidth || height > maxHeight) {
                String limit = "logo".equals(prefix)
                        ? "256×256"
                        : "不超过 " + maxWidth + "×" + maxHeight;
                throw new BizException(ErrorCode.FILE_INVALID, "图片尺寸需为 " + limit);
            }
            if ("logo".equals(prefix) && (width != LOGO_SIZE || height != LOGO_SIZE)) {
                throw new BizException(ErrorCode.FILE_INVALID, "Logo 需为 256×256 的 JPG / PNG 图片");
            }

            String objectName = "system/" + prefix + "-" + UUID.randomUUID() + "." + extension;
            return storageService.upload(file.getInputStream(), file.getSize(), contentType, objectName);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片读取失败");
        }
    }
}
