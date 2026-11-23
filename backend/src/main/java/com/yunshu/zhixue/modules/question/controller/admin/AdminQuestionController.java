package com.yunshu.zhixue.modules.question.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.question.dto.BatchAuditRequest;
import com.yunshu.zhixue.modules.question.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.question.dto.BatchQuestionStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionDetailVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportResultVO;
import com.yunshu.zhixue.modules.question.dto.QuestionListItemVO;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsVO;
import com.yunshu.zhixue.modules.question.dto.QuestionStatusRequest;
import com.yunshu.zhixue.modules.question.service.QuestionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 管理端-题库管理（A-Q01 ~ A-Q15）。
 */
@Tag(name = "管理端-题库管理")
@RestController
@RequestMapping("/api/admin/v1/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final QuestionService questionService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "题库统计卡")
    @GetMapping("/stats")
    @RequirePermission("question:view")
    public ApiResponse<QuestionStatsVO> stats() {
        return ApiResponse.ok(questionService.stats());
    }

    @Operation(summary = "题目分页列表")
    @GetMapping
    @RequirePermission("question:view")
    public ApiResponse<PageResult<QuestionListItemVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(questionService.page(keyword, subjectId, stageId,
                type, difficulty, status, page, pageSize));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    @RequirePermission("question:view")
    public ApiResponse<QuestionDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.detail(id));
    }

    @Operation(summary = "新增题目")
    @PostMapping
    @RequirePermission("question:add")
    public ApiResponse<Void> create(@Valid @RequestBody QuestionSaveRequest request) {
        questionService.create(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑题目")
    @PutMapping("/{id}")
    @RequirePermission("question:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody QuestionSaveRequest request) {
        questionService.update(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    @RequirePermission("question:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "审核题目")
    @PostMapping("/{id}/audit")
    @RequirePermission("question:audit")
    public ApiResponse<Void> audit(@PathVariable Long id,
                                   @Valid @RequestBody QuestionAuditRequest request) {
        questionService.audit(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "上架/下架")
    @PostMapping("/{id}/status")
    @RequirePermission("question:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody QuestionStatusRequest request) {
        questionService.updateStatus(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "驳回后重新提交")
    @PostMapping("/{id}/resubmit")
    @RequirePermission("question:edit")
    public ApiResponse<Void> resubmit(@PathVariable Long id) {
        questionService.resubmit(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量上架/下架")
    @PostMapping("/batch-status")
    @RequirePermission("question:edit")
    public ApiResponse<Integer> batchUpdateStatus(
            @Valid @RequestBody BatchQuestionStatusRequest request) {
        return ApiResponse.ok(questionService.batchUpdateStatus(request, actorResolver.resolve()));
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    @RequirePermission("question:delete")
    public ApiResponse<Integer> batchDelete(@Valid @RequestBody BatchIdsRequest request) {
        return ApiResponse.ok(questionService.batchDelete(request, actorResolver.resolve()));
    }

    @Operation(summary = "批量审核")
    @PostMapping("/batch-audit")
    @RequirePermission("question:audit")
    public ApiResponse<Integer> batchAudit(@Valid @RequestBody BatchAuditRequest request) {
        return ApiResponse.ok(questionService.batchAudit(request, actorResolver.resolve()));
    }

    @Operation(summary = "Excel 导入-解析预览")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("question:import")
    public ApiResponse<QuestionImportPreviewVO> previewImport(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(questionService.previewImport(file, actorResolver.resolve()));
    }

    @Operation(summary = "Excel 导入-确认入库")
    @PostMapping("/import/confirm")
    @RequirePermission("question:import")
    public ApiResponse<QuestionImportResultVO> confirmImport(
            @Valid @RequestBody QuestionImportConfirmRequest request) {
        return ApiResponse.ok(questionService.confirmImport(request.token(),
                actorResolver.resolve()));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/import/template")
    @RequirePermission("question:import")
    public ResponseEntity<byte[]> template() {
        return excelResponse(questionService.template(), "题库导入模板.xlsx");
    }

    @Operation(summary = "按筛选导出")
    @GetMapping("/export")
    @RequirePermission("question:export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer status) {
        byte[] data = questionService.export(keyword, subjectId, stageId, type, difficulty,
                status, actorResolver.resolve());
        String filename = "题库导出_"
                + LocalDate.now(TimeUtils.SHANGHAI).format(DateTimeFormatter.BASIC_ISO_DATE)
                + ".xlsx";
        return excelResponse(data, filename);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] data, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(XLSX)
                .body(data);
    }
}
