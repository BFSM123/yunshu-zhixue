package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.FileUploadResult;
import com.yunshu.zhixue.modules.system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端-文件上传（A-F01、2.10）。
 */
@Tag(name = "管理端-文件上传")
@RestController
@RequestMapping("/api/admin/v1/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileService fileService;

    @Operation(summary = "上传 Logo（256×256）")
    @PostMapping("/image")
    public ApiResponse<FileUploadResult> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(new FileUploadResult(fileService.uploadLogo(file)));
    }

    @Operation(summary = "上传默认头像（≤200×200）")
    @PostMapping("/avatar")
    public ApiResponse<FileUploadResult> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(new FileUploadResult(fileService.uploadAvatar(file)));
    }
}
