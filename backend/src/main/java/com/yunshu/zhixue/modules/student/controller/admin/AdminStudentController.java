package com.yunshu.zhixue.modules.student.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.student.dto.StudentBatchIdsRequest;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentDetailVO;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewVO;
import com.yunshu.zhixue.modules.student.dto.StudentStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentVO;
import com.yunshu.zhixue.modules.student.service.StudentService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 管理端-学员管理（A-U01 ~ A-U08）。学员由前台注册产生，不含新增与编辑。
 */
@Tag(name = "管理端-学员管理")
@RestController
@RequestMapping("/api/admin/v1/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StudentService studentService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "学员列表")
    @GetMapping
    @RequirePermission("student:view")
    public ApiResponse<PageResult<StudentVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(studentService.page(keyword, status, page, pageSize));
    }

    @Operation(summary = "学员详情")
    @GetMapping("/{id}/detail")
    @RequirePermission("student:view")
    public ApiResponse<StudentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(studentService.detail(id));
    }

    @Operation(summary = "学员面试记录")
    @GetMapping("/{id}/interviews")
    @RequirePermission("student:view")
    public ApiResponse<PageResult<StudentInterviewVO>> interviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(studentService.interviews(id, page, pageSize));
    }

    @Operation(summary = "启用/禁用学员")
    @PutMapping("/{id}/status")
    @RequirePermission("student:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody StudentStatusRequest request) {
        studentService.updateStatus(id, request.status(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量启用/禁用")
    @PutMapping("/batch-status")
    @RequirePermission("student:edit")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody StudentBatchStatusRequest request) {
        studentService.batchStatus(request.ids(), request.status(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除学员")
    @DeleteMapping("/{id}")
    @RequirePermission("student:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除学员")
    @DeleteMapping("/batch")
    @RequirePermission("student:delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody StudentBatchIdsRequest request) {
        studentService.batchDelete(request.ids(), actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "按筛选导出学员列表")
    @GetMapping("/export")
    @RequirePermission("student:view")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        byte[] data = studentService.export(keyword, status, actorResolver.resolve());
        String filename = "学员列表_"
                + LocalDate.now(TimeUtils.SHANGHAI).format(DateTimeFormatter.BASIC_ISO_DATE)
                + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(XLSX)
                .body(data);
    }
}