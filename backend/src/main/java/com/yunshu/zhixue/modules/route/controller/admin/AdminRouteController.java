package com.yunshu.zhixue.modules.route.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.route.dto.AddRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.CreateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.RouteCardVO;
import com.yunshu.zhixue.modules.route.dto.RouteDetailVO;
import com.yunshu.zhixue.modules.route.dto.SortRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteStatusRequest;
import com.yunshu.zhixue.modules.route.service.RouteService;
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
 * 管理端-学习路线管理（A-R01 ~ A-R09）。
 */
@Tag(name = "管理端-学习路线管理")
@RestController
@RequestMapping("/api/admin/v1/routes")
@RequiredArgsConstructor
public class AdminRouteController {

    private final RouteService routeService;
    private final CurrentActorResolver actorResolver;

    @Operation(summary = "路线卡片列表")
    @GetMapping
    @RequirePermission("route:view")
    public ApiResponse<List<RouteCardVO>> list() {
        return ApiResponse.ok(routeService.list());
    }

    @Operation(summary = "新建路线")
    @PostMapping
    @RequirePermission("route:add")
    public ApiResponse<Void> create(@Valid @RequestBody CreateRouteRequest request) {
        routeService.create(request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑路线基本信息")
    @PutMapping("/{id}")
    @RequirePermission("route:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateRouteRequest request) {
        routeService.update(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "路线启用/禁用")
    @PostMapping("/{id}/status")
    @RequirePermission("route:edit")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateRouteStatusRequest request) {
        routeService.updateStatus(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除路线")
    @DeleteMapping("/{id}")
    @RequirePermission("route:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        routeService.delete(id, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "路线详情")
    @GetMapping("/{id}/detail")
    @RequirePermission("route:view")
    public ApiResponse<RouteDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(routeService.detail(id));
    }

    @Operation(summary = "从课程库添加课程")
    @PostMapping("/{id}/courses")
    @RequirePermission("route:edit")
    public ApiResponse<Void> addCourses(@PathVariable Long id,
                                        @Valid @RequestBody AddRouteCoursesRequest request) {
        routeService.addCourses(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "移除路线课程")
    @DeleteMapping("/{id}/courses/{courseId}")
    @RequirePermission("route:edit")
    public ApiResponse<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        routeService.removeCourse(id, courseId, actorResolver.resolve());
        return ApiResponse.ok();
    }

    @Operation(summary = "路线课程排序")
    @PutMapping("/{id}/courses/sort")
    @RequirePermission("route:edit")
    public ApiResponse<Void> sortCourses(@PathVariable Long id,
                                         @Valid @RequestBody SortRouteCoursesRequest request) {
        routeService.sortCourses(id, request, actorResolver.resolve());
        return ApiResponse.ok();
    }
}
