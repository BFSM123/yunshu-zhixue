package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 管理端-操作日志（A-G01 ~ A-G04）。
 */
@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/api/admin/v1/logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final OperationLogService operationLogService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "日志列表")
    @GetMapping
    @RequirePermission("system:log_view")
    public ApiResponse<PageResult<OperationLogVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(operationLogService.page(keyword, operationType, startDate, endDate,
                page, pageSize));
    }

    @Operation(summary = "日志详情")
    @GetMapping("/{id}")
    @RequirePermission("system:log_view")
    public ApiResponse<OperationLogVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.detail(id));
    }

    @Operation(summary = "导出日志 CSV")
    @GetMapping("/export")
    @RequirePermission("system:log_view")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] data = operationLogService.exportCsv(keyword, operationType, startDate, endDate,
                actorResolver.resolve());
        String filename = "operation-logs-"
                + LocalDate.now(TimeUtils.SHANGHAI).format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }

    @Operation(summary = "清空日志")
    @DeleteMapping
    @RequirePermission("system:log_view")
    public ApiResponse<Void> clear() {
        operationLogService.clearAll(actorResolver.resolve());
        return ApiResponse.ok();
    }
}
