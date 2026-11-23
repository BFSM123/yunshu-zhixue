package com.yunshu.zhixue.modules.interviewbank.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.BatchInterviewStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewBatchIdsRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPreviewVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportResultVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetailVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionListItemVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicVO;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewBankService;
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
import java.util.List;

/**
 * 管理端-面试题库管理（A-I01 ~ A-I13，含上下架与 Excel 导入）。
 */
@Tag(name = "管理端-面试题库管理")
@RestController
@RequestMapping("/api/admin/v1")
@RequiredArgsConstructor
public class AdminInterviewBankController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final InterviewBankService interviewBankService;
    private final CurrentActorResolver actorResolver;

    // ==================== 面试题 ====================

    @Operation(summary = "面试题分页列表")
    @GetMapping("/interview-questions")
    @RequirePermission("interviewbank:view")
    public ApiResponse<PageResult<InterviewQuestionListItemVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Boolean unclassified,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(interviewBankService.page(keyword, subjectId, topic,
                difficulty, unclassified, page, pageSize));
    }

    @Operation(summary = "面试题详情")
    @GetMapping("/interview-questions/{id}")
    @RequirePermission("interviewbank:view")
    public ApiResponse<InterviewQuestionDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(interviewBankService.detail(id));
    }

    @Operation(summary = "手工录入面试题")
    @PostMapping("/interview-questions")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> create(@Valid @RequestBody InterviewQuestionSaveRequest request) {
        interviewBankService.create(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑面试题（仅主题与难度）")
    @PutMapping("/interview-questions/{id}")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody InterviewQuestionUpdateRequest request) {
        interviewBankService.update(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除面试题")
    @DeleteMapping("/interview-questions/{id}")
    @RequirePermission("interviewbank:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        interviewBankService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "面试题上架/下架")
    @PostMapping("/interview-questions/{id}/status")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody InterviewQuestionStatusRequest request) {
        interviewBankService.updateStatus(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量上架/下架")
    @PostMapping("/interview-questions/batch-status")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Integer> batchUpdateStatus(
            @Valid @RequestBody BatchInterviewStatusRequest request) {
        return ApiResponse.ok(interviewBankService.batchUpdateStatus(
                request, actorResolver.resolve()));
    }

    @Operation(summary = "批量删除面试题")
    @PostMapping("/interview-questions/batch-delete")
    @RequirePermission("interviewbank:delete")
    public ApiResponse<Integer> batchDelete(@Valid @RequestBody InterviewBatchIdsRequest request) {
        return ApiResponse.ok(interviewBankService.batchDelete(request, actorResolver.resolve()));
    }

    @Operation(summary = "Excel 导入-解析预览")
    @PostMapping(value = "/interview-questions/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("interviewbank:edit")
    public ApiResponse<InterviewImportPreviewVO> previewImport(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(interviewBankService.previewImport(file, actorResolver.resolve()));
    }

    @Operation(summary = "Excel 导入-确认入库")
    @PostMapping("/interview-questions/import/confirm")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<InterviewImportResultVO> confirmImport(
            @Valid @RequestBody InterviewImportConfirmRequest request) {
        return ApiResponse.ok(interviewBankService.confirmImport(request.token(),
                actorResolver.resolve()));
    }

    @Operation(summary = "下载面试题导入模板")
    @GetMapping("/interview-questions/import/template")
    @RequirePermission("interviewbank:view")
    public ResponseEntity<byte[]> template() {
        return excelResponse(interviewBankService.template(), "面试题导入模板.xlsx");
    }

    @Operation(summary = "关键词检索测试")
    @PostMapping("/interview-questions/search-test")
    @RequirePermission("interviewbank:search_test")
    public ApiResponse<InterviewSearchTestVO> searchTest(
            @Valid @RequestBody InterviewSearchTestRequest request) {
        return ApiResponse.ok(interviewBankService.searchTest(request));
    }

    // ==================== 主题词表 ====================

    @Operation(summary = "主题列表（含 0 题主题与词表外主题）")
    @GetMapping("/interview-topics")
    @RequirePermission("interviewbank:view")
    public ApiResponse<List<InterviewTopicVO>> topics() {
        return ApiResponse.ok(interviewBankService.topics());
    }

    @Operation(summary = "新增主题")
    @PostMapping("/interview-topics")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> createTopic(@Valid @RequestBody InterviewTopicSaveRequest request) {
        interviewBankService.createTopic(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑主题")
    @PutMapping("/interview-topics/{id}")
    @RequirePermission("interviewbank:edit")
    public ApiResponse<Void> updateTopic(@PathVariable Long id,
                                         @Valid @RequestBody InterviewTopicSaveRequest request) {
        interviewBankService.updateTopic(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除主题")
    @DeleteMapping("/interview-topics/{id}")
    @RequirePermission("interviewbank:delete")
    public ApiResponse<Void> deleteTopic(@PathVariable Long id) {
        interviewBankService.deleteTopic(id, actorResolver.resolve());
        return ApiResponse.ok();
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