package com.yunshu.zhixue.modules.system.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端图片上传（Logo / 默认头像）。
 */
public interface FileService {

    String uploadLogo(MultipartFile file);

    String uploadAvatar(MultipartFile file);
}
