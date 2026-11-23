package com.yunshu.zhixue.modules.route.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.route.dto.AddRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.CreateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.RouteCardVO;
import com.yunshu.zhixue.modules.route.dto.RouteCoursePreviewVO;
import com.yunshu.zhixue.modules.route.dto.RouteCourseVO;
import com.yunshu.zhixue.modules.route.dto.RouteDetailVO;
import com.yunshu.zhixue.modules.route.dto.SortRouteCoursesRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteRequest;
import com.yunshu.zhixue.modules.route.dto.UpdateRouteStatusRequest;
import com.yunshu.zhixue.modules.route.entity.LearningRoute;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.mapper.LearningRouteMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseMapper;
import com.yunshu.zhixue.modules.route.service.RouteService;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习路线管理实现。
 *
 * <p>路线与课程为多对多关系，route_course.sort_order 即学习顺序。
 * 关联表与学员完成记录在删除时物理清理，避免逻辑删除行占用唯一键。</p>
 */
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final String TARGET_ROUTE = "学习路线";
    private static final String OTHER_SUBJECT_NAME = "其他学科";
    private static final String UNKNOWN_SUBJECT_NAME = "未知学科";
    private static final int PREVIEW_COURSE_COUNT = 4;
    private static final int STATUS_ENABLED = 1;

    private final LearningRouteMapper routeMapper;
    private final RouteCourseMapper routeCourseMapper;
    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectMapper subjectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    // ==================== 查询 ====================

    @Override
    public List<RouteCardVO> list() {
        List<LearningRoute> routes = routeMapper.selectList(
                Wrappers.lambdaQuery(LearningRoute.class).orderByDesc(LearningRoute::getId));
        if (routes.isEmpty()) {
            return List.of();
        }

        List<Long> routeIds = routes.stream().map(LearningRoute::getId).toList();
        List<RouteCourse> relations = listRelations(routeIds);
        Map<Long, List<RouteCourse>> relationsByRoute = relations.stream()
                .collect(Collectors.groupingBy(RouteCourse::getRouteId, LinkedHashMap::new,
                        Collectors.toList()));

        Set<Long> courseIds = relations.stream().map(RouteCourse::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, Course> courseMap = loadCourses(courseIds);
        Map<Long, String> subjectNames = loadSubjectNames(
                routes.stream().map(LearningRoute::getSubjectId).collect(Collectors.toSet()));

        return routes.stream().map(route -> {
            List<RouteCourse> routeCourses = relationsByRoute
                    .getOrDefault(route.getId(), List.of()).stream()
                    .filter(relation -> courseMap.containsKey(relation.getCourseId()))
                    .toList();
            long totalDuration = routeCourses.stream()
                    .mapToLong(relation -> durationOf(courseMap.get(relation.getCourseId())))
                    .sum();
            List<RouteCoursePreviewVO> previews = routeCourses.stream()
                    .limit(PREVIEW_COURSE_COUNT)
                    .map(relation -> {
                        Course course = courseMap.get(relation.getCourseId());
                        return new RouteCoursePreviewVO(
                                relation.getId(),
                                relation.getCourseId(),
                                course.getName(),
                                course.getBvid(),
                                course.getDurationMinutes());
                    })
                    .toList();
            return new RouteCardVO(
                    route.getId(),
                    route.getName(),
                    route.getSubjectId(),
                    subjectName(route.getSubjectId(), subjectNames),
                    route.getStatus(),
                    route.getIcon(),
                    route.getTargetEducation(),
                    route.getDescription(),
                    routeCourses.size(),
                    totalDuration,
                    previews);
        }).toList();
    }

    @Override
    public RouteDetailVO detail(Long id) {
        LearningRoute route = requireRoute(id);
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class)
                        .eq(RouteCourse::getRouteId, id)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));

        Set<Long> courseIds = relations.stream().map(RouteCourse::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, Course> courseMap = loadCourses(courseIds);
        Map<Long, String> subjectNames = loadSubjectNames(
                courseMap.values().stream().map(Course::getSubjectId).collect(Collectors.toSet()));
        Map<Long, Set<Long>> courseSubjectIds = loadCourseSubjectIds(courseIds);
        String routeSubjectName = subjectName(route.getSubjectId(), subjectNames);

        List<RouteCourseVO> courses = new ArrayList<>();
        long totalDuration = 0L;
        for (RouteCourse relation : relations) {
            Course course = courseMap.get(relation.getCourseId());
            if (course == null) {
                continue;
            }
            totalDuration += durationOf(course);
            boolean otherSubject = !belongsToSubject(course, relation.getCourseId(),
                    route.getSubjectId(), courseSubjectIds);
            courses.add(new RouteCourseVO(
                    relation.getId(),
                    course.getId(),
                    course.getName(),
                    course.getBvid(),
                    course.getDurationMinutes(),
                    course.getSource(),
                    course.getCoverUrl(),
                    course.getSubjectId(),
                    subjectName(course.getSubjectId(), subjectNames),
                    relation.getSortOrder() == null ? courses.size() + 1 : relation.getSortOrder(),
                    otherSubject));
        }

        return new RouteDetailVO(
                route.getId(),
                route.getName(),
                route.getSubjectId(),
                routeSubjectName,
                route.getStatus(),
                route.getIcon(),
                route.getTargetEducation(),
                route.getDescription(),
                courses.size(),
                totalDuration,
                courses);
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional
    public void create(CreateRouteRequest request, Actor actor) {
        String name = request.name().trim();
        requireSubject(request.subjectId());

        LearningRoute route = new LearningRoute();
        route.setName(name);
        route.setSubjectId(request.subjectId());
        route.setStatus(request.status() == null ? STATUS_ENABLED : request.status());
        route.setIcon(normalizeOptional(request.icon()));
        route.setTargetEducation(normalizeOptional(request.targetEducation()));
        route.setDescription(normalizeOptional(request.description()));
        routeMapper.insert(route);
        audit(actor, "新增", name);
    }

    @Override
    @Transactional
    public void update(Long id, UpdateRouteRequest request, Actor actor) {
        LearningRoute route = requireRoute(id);
        requireSubject(request.subjectId());
        String name = request.name().trim();
        Integer status = request.status() == null ? route.getStatus() : request.status();

        // updateById 默认忽略 null，这里显式 set 以支持清空图标/学历/描述
        routeMapper.update(null, Wrappers.lambdaUpdate(LearningRoute.class)
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getName, name)
                .set(LearningRoute::getSubjectId, request.subjectId())
                .set(LearningRoute::getStatus, status)
                .set(LearningRoute::getIcon, normalizeOptional(request.icon()))
                .set(LearningRoute::getTargetEducation, normalizeOptional(request.targetEducation()))
                .set(LearningRoute::getDescription, normalizeOptional(request.description())));
        audit(actor, "修改", name);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, UpdateRouteStatusRequest request, Actor actor) {
        LearningRoute route = requireRoute(id);
        routeMapper.update(null, Wrappers.lambdaUpdate(LearningRoute.class)
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getStatus, request.status()));
        audit(actor, request.status() == STATUS_ENABLED ? "启用" : "禁用", route.getName());
    }

    @Override
    @Transactional
    public void delete(Long id, Actor actor) {
        LearningRoute route = requireRoute(id);
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class).eq(RouteCourse::getRouteId, id));
        deleteCompletions(relations.stream().map(RouteCourse::getId).toList());
        jdbcTemplate.update("DELETE FROM route_course WHERE route_id = ?", id);
        routeMapper.deleteById(id);
        audit(actor, "删除", route.getName());
    }

    // ==================== 路线课程 ====================

    @Override
    @Transactional
    public void addCourses(Long id, AddRouteCoursesRequest request, Actor actor) {
        LearningRoute route = requireRoute(id);
        LinkedHashSet<Long> courseIds = request.courseIds().stream()
                .filter(Objects::nonNull)
                .filter(courseId -> courseId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (courseIds.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "请选择要添加的课程");
        }

        Map<Long, Course> courseMap = loadCourses(courseIds);
        if (courseMap.size() != courseIds.size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "部分课程不存在或已删除");
        }

        List<RouteCourse> existing = routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class).eq(RouteCourse::getRouteId, id));
        Set<Long> existingCourseIds = existing.stream().map(RouteCourse::getCourseId)
                .collect(Collectors.toSet());
        if (!Collections.disjoint(courseIds, existingCourseIds)) {
            throw new BizException(ErrorCode.CONFLICT, "所选课程已在本路线中，请勿重复添加");
        }

        int nextOrder = existing.stream()
                .map(RouteCourse::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        try {
            for (Long courseId : courseIds) {
                RouteCourse relation = new RouteCourse();
                relation.setRouteId(id);
                relation.setCourseId(courseId);
                relation.setSortOrder(nextOrder++);
                routeCourseMapper.insert(relation);
            }
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "所选课程已在本路线中，请勿重复添加");
        }
        audit(actor, "添加课程", route.getName() + "（" + courseIds.size() + " 门）");
    }

    @Override
    @Transactional
    public void removeCourse(Long id, Long courseId, Actor actor) {
        LearningRoute route = requireRoute(id);
        RouteCourse relation = routeCourseMapper.selectOne(
                Wrappers.lambdaQuery(RouteCourse.class)
                        .eq(RouteCourse::getRouteId, id)
                        .eq(RouteCourse::getCourseId, courseId)
                        .last("LIMIT 1"));
        if (relation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该课程不在本路线中");
        }

        Course course = courseMapper.selectById(courseId);
        deleteCompletions(List.of(relation.getId()));
        jdbcTemplate.update("DELETE FROM route_course WHERE id = ?", relation.getId());
        compactSortOrder(id);
        audit(actor, "移除课程", route.getName() + " - "
                + (course == null ? "课程#" + courseId : course.getName()));
    }

    @Override
    @Transactional
    public void sortCourses(Long id, SortRouteCoursesRequest request, Actor actor) {
        LearningRoute route = requireRoute(id);
        List<Long> requestedIds = request.routeCourseIds();
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>(requestedIds);
        List<RouteCourse> existing = routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class).eq(RouteCourse::getRouteId, id));
        Set<Long> existingIds = existing.stream().map(RouteCourse::getId)
                .collect(Collectors.toSet());

        if (distinctIds.size() != requestedIds.size()
                || distinctIds.size() != existingIds.size()
                || !existingIds.equals(distinctIds)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "排序列表与路线课程不一致，请刷新后重试");
        }

        int order = 1;
        for (Long routeCourseId : requestedIds) {
            jdbcTemplate.update("UPDATE route_course SET sort_order = ? WHERE id = ? AND route_id = ?",
                    order++, routeCourseId, id);
        }
        audit(actor, "课程排序", route.getName());
    }

    // ==================== 内部工具 ====================

    private LearningRoute requireRoute(Long id) {
        LearningRoute route = routeMapper.selectById(id);
        if (route == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习路线不存在");
        }
        return route;
    }

    private void requireSubject(Long id) {
        if (subjectMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
    }

    private List<RouteCourse> listRelations(List<Long> routeIds) {
        return routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class)
                        .in(RouteCourse::getRouteId, routeIds)
                        .orderByAsc(RouteCourse::getRouteId)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
    }

    private Map<Long, Course> loadCourses(Set<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinctIds = courseIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(),
                        (left, right) -> left));
    }

    private Map<Long, String> loadSubjectNames(Set<Long> subjectIds) {
        Set<Long> distinctIds = subjectIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName,
                        (left, right) -> left));
    }

    /**
     * 课程关联的额外学科，用于判断课程是否属于路线学科。
     */
    private Map<Long, Set<Long>> loadCourseSubjectIds(Set<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<CourseSubject> relations = courseSubjectMapper.selectList(
                Wrappers.lambdaQuery(CourseSubject.class)
                        .in(CourseSubject::getCourseId, courseIds));
        Map<Long, Set<Long>> result = new HashMap<>();
        for (CourseSubject relation : relations) {
            result.computeIfAbsent(relation.getCourseId(), key -> new HashSet<>())
                    .add(relation.getSubjectId());
        }
        return result;
    }

    private boolean belongsToSubject(Course course, Long courseId, Long routeSubjectId,
                                     Map<Long, Set<Long>> courseSubjectIds) {
        if (routeSubjectId == null) {
            return false;
        }
        if (Objects.equals(course.getSubjectId(), routeSubjectId)) {
            return true;
        }
        return courseSubjectIds.getOrDefault(courseId, Set.of()).contains(routeSubjectId);
    }

    private String subjectName(Long subjectId, Map<Long, String> subjectNames) {
        if (subjectId == null) {
            return OTHER_SUBJECT_NAME;
        }
        return subjectNames.getOrDefault(subjectId, UNKNOWN_SUBJECT_NAME);
    }

    private long durationOf(Course course) {
        return course.getDurationMinutes() == null ? 0L : course.getDurationMinutes();
    }

    private void compactSortOrder(Long routeId) {
        List<RouteCourse> remaining = routeCourseMapper.selectList(
                Wrappers.lambdaQuery(RouteCourse.class)
                        .eq(RouteCourse::getRouteId, routeId)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
        int order = 1;
        for (RouteCourse relation : remaining) {
            if (!Objects.equals(relation.getSortOrder(), order)) {
                jdbcTemplate.update("UPDATE route_course SET sort_order = ? WHERE id = ?",
                        order, relation.getId());
            }
            order++;
        }
    }

    private void deleteCompletions(List<Long> routeCourseIds) {
        if (routeCourseIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(routeCourseIds.size(), "?"));
        jdbcTemplate.update("DELETE FROM route_course_completion WHERE route_course_id IN ("
                + placeholders + ")", routeCourseIds.toArray());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action,
                TARGET_ROUTE, targetName, actor.ip());
    }
}
