package com.yunshu.zhixue.modules.subject.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.subject.dto.CreateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.CreateSubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCardVO;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsVO;
import com.yunshu.zhixue.modules.subject.dto.UpdateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.UpdateStageStatusRequest;
import com.yunshu.zhixue.modules.subject.dto.UpdateSubjectRequest;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-学科管理（A-S01 ~ A-S11，阶段管理随学科权限点）。
 */
@Tag(name = "管理端-学科管理")
@RestController
@RequestMapping("/api/admin/v1/subjects")
@RequiredArgsConstructor
public class AdminSubjectController {

    private final SubjectService subjectService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "学科统计卡")
    @GetMapping("/stats")
    @RequirePermission("subject:view")
    public ApiResponse<SubjectStatsVO> stats() {
        return ApiResponse.ok(subjectService.stats());
    }

    @Operation(summary = "学科卡片列表")
    @GetMapping
    @RequirePermission("subject:view")
    public ApiResponse<List<SubjectCardVO>> list() {
        return ApiResponse.ok(subjectService.list());
    }

    @Operation(summary = "新增学科")
    @PostMapping
    @RequirePermission("subject:add")
    public ApiResponse<Void> create(@Valid @RequestBody CreateSubjectRequest request) {
        subjectService.create(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑学科")
    @PutMapping("/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateSubjectRequest request) {
        subjectService.update(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "学科上移/下移")
    @PutMapping("/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sort(@Valid @RequestBody SortRequest request) {
        subjectService.sort(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除学科")
    @DeleteMapping("/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "新增阶段")
    @PostMapping("/{subjectId}/stages")
    @RequirePermission("subject:add")
    public ApiResponse<Void> createStage(@PathVariable Long subjectId,
                                         @Valid @RequestBody CreateStageRequest request) {
        subjectService.createStage(subjectId, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑阶段")
    @PutMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStage(@PathVariable Long subjectId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody UpdateStageRequest request) {
        subjectService.updateStage(subjectId, id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "阶段上移/下移")
    @PutMapping("/{subjectId}/stages/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sortStage(@PathVariable Long subjectId,
                                       @Valid @RequestBody SortRequest request) {
        subjectService.sortStage(subjectId, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "阶段启用/禁用")
    @PutMapping("/{subjectId}/stages/{id}/status")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStageStatus(@PathVariable Long subjectId,
                                               @PathVariable Long id,
                                               @Valid @RequestBody UpdateStageStatusRequest request) {
        subjectService.updateStageStatus(subjectId, id, request.status(),
                actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除阶段")
    @DeleteMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> deleteStage(@PathVariable Long subjectId,
                                         @PathVariable Long id) {
        subjectService.deleteStage(subjectId, id, actorResolver.resolve());
        return ApiResponse.ok();
    }
}
