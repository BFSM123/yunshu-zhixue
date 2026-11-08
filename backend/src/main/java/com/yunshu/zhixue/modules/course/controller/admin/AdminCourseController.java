package com.yunshu.zhixue.modules.course.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmRequest;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewVO;
import com.yunshu.zhixue.modules.course.dto.CourseImportResultVO;
import com.yunshu.zhixue.modules.course.dto.CourseStatsVO;
import com.yunshu.zhixue.modules.course.dto.CourseVO;
import com.yunshu.zhixue.modules.course.dto.CreateCourseRequest;
import com.yunshu.zhixue.modules.course.dto.UpdateCourseRequest;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.modules.system.support.CurrentActorResolver;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

/**
 * 管理端-B 站课程库（A-C01 ~ A-C07）。
 */
@Tag(name = "管理端-课程库")
@RestController
@RequestMapping("/api/admin/v1/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "课程库统计卡")
    @GetMapping("/stats")
    @RequirePermission("course:view")
    public ApiResponse<CourseStatsVO> stats() {
        return ApiResponse.ok(courseService.stats());
    }

    @Operation(summary = "课程分页列表")
    @GetMapping
    @RequirePermission("course:view")
    public ApiResponse<PageResult<CourseVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer source,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(courseService.page(keyword, subjectId, source, page, pageSize));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @RequirePermission("course:add")
    public ApiResponse<Void> create(@Valid @RequestBody CreateCourseRequest request) {
        courseService.create(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑课程")
    @PutMapping("/{id}")
    @RequirePermission("course:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateCourseRequest request) {
        courseService.update(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @RequirePermission("course:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "批量导入-解析预览")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:add")
    public ApiResponse<CourseImportPreviewVO> previewImport(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(courseService.previewImport(file, actorResolver.resolve()));
    }

    @Operation(summary = "批量导入-确认入库")
    @PostMapping("/import/confirm")
    @RequirePermission("course:add")
    public ApiResponse<CourseImportResultVO> confirmImport(
            @Valid @RequestBody CourseImportConfirmRequest request) {
        return ApiResponse.ok(courseService.confirmImport(request.token(), actorResolver.resolve()));
    }
}
