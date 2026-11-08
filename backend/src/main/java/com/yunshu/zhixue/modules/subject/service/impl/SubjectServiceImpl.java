package com.yunshu.zhixue.modules.subject.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.subject.dto.CreateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.CreateSubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.StageVO;
import com.yunshu.zhixue.modules.subject.dto.SubjectCardVO;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsVO;
import com.yunshu.zhixue.modules.subject.dto.UpdateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.UpdateSubjectRequest;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学科与阶段管理实现。
 *
 * <p>课程、路线、题目模块尚未落地实体，关联计数与删除校验统一使用 JdbcTemplate
 * 直接聚合，避免在学科模块内反向依赖其他业务模块。</p>
 */
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private static final String TARGET_SUBJECT = "学科";
    private static final String TARGET_STAGE = "阶段";
    private static final String DEFAULT_THEME_COLOR = "#2563EB";

    private final SubjectMapper subjectMapper;
    private final StageMapper stageMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    // ==================== 查询 ====================

    @Override
    public SubjectStatsVO stats() {
        long totalSubjects = count("SELECT COUNT(*) FROM subject WHERE delete_flag = 0");
        long subjectsWithStages = count(
                "SELECT COUNT(DISTINCT subject_id) FROM stage WHERE delete_flag = 0");
        long totalStages = count("SELECT COUNT(*) FROM stage WHERE delete_flag = 0");
        long totalQuestions = count("SELECT COUNT(*) FROM question WHERE delete_flag = 0");
        return new SubjectStatsVO(totalSubjects, subjectsWithStages, totalStages, totalQuestions);
    }

    @Override
    public List<SubjectCardVO> list() {
        List<Subject> subjects = subjectMapper.selectList(
                Wrappers.lambdaQuery(Subject.class)
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId));
        if (subjects.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> courseCounts = countByFirstColumn("""
                SELECT t.subject_id, COUNT(DISTINCT t.course_id)
                FROM (
                    SELECT id AS course_id, subject_id
                    FROM course
                    WHERE delete_flag = 0 AND subject_id IS NOT NULL
                    UNION ALL
                    SELECT course_id, subject_id
                    FROM course_subject
                    WHERE delete_flag = 0
                ) t
                GROUP BY t.subject_id
                """);
        Map<Long, Long> routeCounts = countByFirstColumn(
                "SELECT subject_id, COUNT(*) FROM learning_route "
                        + "WHERE delete_flag = 0 GROUP BY subject_id");
        Map<Long, Long> stageQuestionCounts = countByFirstColumn(
                "SELECT stage_id, COUNT(*) FROM question WHERE delete_flag = 0 GROUP BY stage_id");

        List<Stage> stages = stageMapper.selectList(
                Wrappers.lambdaQuery(Stage.class)
                        .orderByAsc(Stage::getSubjectId)
                        .orderByAsc(Stage::getSortOrder)
                        .orderByAsc(Stage::getId));
        Map<Long, List<Stage>> stagesBySubject = stages.stream()
                .collect(Collectors.groupingBy(Stage::getSubjectId, LinkedHashMap::new,
                        Collectors.toList()));

        return subjects.stream().map(subject -> {
            List<StageVO> stageVOs = stagesBySubject.getOrDefault(subject.getId(), List.of())
                    .stream()
                    .map(stage -> new StageVO(
                            stage.getId(),
                            stage.getName(),
                            stage.getIcon(),
                            stage.getSortOrder(),
                            stage.getStatus(),
                            stageQuestionCounts.getOrDefault(stage.getId(), 0L)))
                    .toList();
            long questionCount = stageVOs.stream().mapToLong(StageVO::questionCount).sum();
            return new SubjectCardVO(
                    subject.getId(),
                    subject.getName(),
                    subject.getIcon(),
                    subject.getDescription(),
                    subject.getThemeColor(),
                    subject.getSortOrder(),
                    courseCounts.getOrDefault(subject.getId(), 0L),
                    routeCounts.getOrDefault(subject.getId(), 0L),
                    questionCount,
                    stageVOs);
        }).toList();
    }

    // ==================== 学科 ====================

    @Override
    @Transactional
    public void create(CreateSubjectRequest request, Actor actor) {
        String name = request.name().trim();
        if (nameExists(name, null)) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "学科名称已存在");
        }

        Subject subject = new Subject();
        subject.setName(name);
        subject.setIcon(normalizeRequired(request.icon()));
        subject.setDescription(normalizeRequired(request.description()));
        subject.setThemeColor(normalizeThemeColor(request.themeColor()));
        subject.setSortOrder(request.sortOrder() != null
                ? request.sortOrder()
                : nextSubjectSortOrder());
        try {
            subjectMapper.insert(subject);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "学科名称已存在");
        }
        audit(actor, "新增", TARGET_SUBJECT, subject.getName());
    }

    @Override
    @Transactional
    public void update(Long id, UpdateSubjectRequest request, Actor actor) {
        Subject subject = requireSubject(id);
        String name = request.name().trim();
        if (nameExists(name, id)) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "学科名称已存在");
        }

        subject.setName(name);
        subject.setIcon(normalizeRequired(request.icon()));
        subject.setDescription(normalizeRequired(request.description()));
        subject.setThemeColor(normalizeThemeColor(request.themeColor()));
        if (request.sortOrder() != null) {
            subject.setSortOrder(request.sortOrder());
        }
        try {
            subjectMapper.updateById(subject);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "学科名称已存在");
        }
        audit(actor, "修改", TARGET_SUBJECT, subject.getName());
    }

    @Override
    @Transactional
    public void sort(SortRequest request, Actor actor) {
        Subject subject = requireSubject(request.id());
        List<Subject> ordered = subjectMapper.selectList(
                Wrappers.lambdaQuery(Subject.class)
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId));
        if (!move(ordered, subject.getId(), request.move(), Subject::getId, Subject::setSortOrder)) {
            return;
        }
        ordered.forEach(subjectMapper::updateById);
        audit(actor, "修改", TARGET_SUBJECT, subject.getName());
    }

    @Override
    @Transactional
    public void delete(Long id, Actor actor) {
        Subject subject = requireSubject(id);
        long stageCount = count(
                "SELECT COUNT(*) FROM stage WHERE delete_flag = 0 AND subject_id = ?", id);
        long questionCount = count("""
                SELECT COUNT(*)
                FROM question q
                JOIN stage s ON s.id = q.stage_id AND s.delete_flag = 0
                WHERE q.delete_flag = 0 AND s.subject_id = ?
                """, id);
        long courseCount = count("""
                SELECT COUNT(DISTINCT t.course_id)
                FROM (
                    SELECT id AS course_id FROM course
                    WHERE delete_flag = 0 AND subject_id = ?
                    UNION ALL
                    SELECT course_id FROM course_subject
                    WHERE delete_flag = 0 AND subject_id = ?
                ) t
                """, id, id);
        long routeCount = count(
                "SELECT COUNT(*) FROM learning_route WHERE delete_flag = 0 AND subject_id = ?", id);

        if (stageCount + questionCount + courseCount + routeCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    buildSubjectDeleteMessage(stageCount, questionCount, courseCount, routeCount));
        }

        subjectMapper.deleteById(id);
        audit(actor, "删除", TARGET_SUBJECT, subject.getName());
    }

    // ==================== 阶段 ====================

    @Override
    @Transactional
    public void createStage(Long subjectId, CreateStageRequest request, Actor actor) {
        Subject subject = requireSubject(subjectId);
        String name = request.name().trim();
        if (stageNameExists(subjectId, name, null)) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "该学科下阶段名称已存在");
        }

        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(name);
        stage.setIcon(normalizeOptional(request.icon()));
        stage.setSortOrder(request.sortOrder() != null
                ? request.sortOrder()
                : nextStageSortOrder(subjectId));
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "该学科下阶段名称已存在");
        }
        audit(actor, "新增", TARGET_STAGE, stageLabel(subject, stage));
    }

    @Override
    @Transactional
    public void updateStage(Long subjectId, Long stageId, UpdateStageRequest request, Actor actor) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        String name = request.name().trim();
        if (stageNameExists(subjectId, name, stageId)) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "该学科下阶段名称已存在");
        }

        stage.setName(name);
        stage.setIcon(normalizeOptional(request.icon()));
        if (request.sortOrder() != null) {
            stage.setSortOrder(request.sortOrder());
        }
        try {
            stageMapper.updateById(stage);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "该学科下阶段名称已存在");
        }
        audit(actor, "修改", TARGET_STAGE, stageLabel(subject, stage));
    }

    @Override
    @Transactional
    public void sortStage(Long subjectId, SortRequest request, Actor actor) {
        Subject subject = requireSubject(subjectId);
        requireStage(subjectId, request.id());
        List<Stage> ordered = stagesOf(subjectId);
        if (!move(ordered, request.id(), request.move(), Stage::getId, Stage::setSortOrder)) {
            return;
        }
        ordered.forEach(stageMapper::updateById);
        Stage moved = ordered.stream()
                .filter(stage -> stage.getId().equals(request.id()))
                .findFirst()
                .orElseThrow();
        audit(actor, "修改", TARGET_STAGE, stageLabel(subject, moved));
    }

    @Override
    @Transactional
    public void updateStageStatus(Long subjectId, Long stageId, Integer status, Actor actor) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        stage.setStatus(status);
        stageMapper.updateById(stage);
        audit(actor, "禁用启用", TARGET_STAGE, stageLabel(subject, stage));
    }

    @Override
    @Transactional
    public void deleteStage(Long subjectId, Long stageId, Actor actor) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        long questionCount = count(
                "SELECT COUNT(*) FROM question WHERE delete_flag = 0 AND stage_id = ?", stageId);
        if (questionCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该阶段下存在 " + questionCount + " 道题目，请先处理题目后再删除");
        }
        stageMapper.deleteById(stageId);
        audit(actor, "删除", TARGET_STAGE, stageLabel(subject, stage));
    }

    // ==================== 内部工具 ====================

    private Subject requireSubject(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
        return subject;
    }

    private Stage requireStage(Long subjectId, Long stageId) {
        Stage stage = stageMapper.selectById(stageId);
        if (stage == null || !subjectId.equals(stage.getSubjectId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    private List<Stage> stagesOf(Long subjectId) {
        return stageMapper.selectList(
                Wrappers.lambdaQuery(Stage.class)
                        .eq(Stage::getSubjectId, subjectId)
                        .orderByAsc(Stage::getSortOrder)
                        .orderByAsc(Stage::getId));
    }

    private boolean nameExists(String name, Long excludeId) {
        var wrapper = Wrappers.lambdaQuery(Subject.class).eq(Subject::getName, name);
        if (excludeId != null) {
            wrapper.ne(Subject::getId, excludeId);
        }
        return subjectMapper.selectCount(wrapper) > 0;
    }

    private boolean stageNameExists(Long subjectId, String name, Long excludeId) {
        var wrapper = Wrappers.lambdaQuery(Stage.class)
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, name);
        if (excludeId != null) {
            wrapper.ne(Stage::getId, excludeId);
        }
        return stageMapper.selectCount(wrapper) > 0;
    }

    private int nextSubjectSortOrder() {
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM subject WHERE delete_flag = 0", Long.class);
        return Math.toIntExact((max == null ? 0 : max) + 1);
    }

    private int nextStageSortOrder(Long subjectId) {
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM stage "
                        + "WHERE delete_flag = 0 AND subject_id = ?",
                Long.class, subjectId);
        return Math.toIntExact((max == null ? 0 : max) + 1);
    }

    /**
     * 交换相邻项后整体重排序号（从 1 开始），避免重复排序号导致顺序不稳定。
     */
    private <T> boolean move(List<T> ordered, Long id, String direction,
                             Function<T, Long> idGetter,
                             java.util.function.BiConsumer<T, Integer> sortSetter) {
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (idGetter.apply(ordered.get(i)).equals(id)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "排序对象不存在");
        }
        int target = "up".equals(direction) ? index - 1 : index + 1;
        if (target < 0 || target >= ordered.size()) {
            return false;
        }
        T current = ordered.get(index);
        ordered.set(index, ordered.get(target));
        ordered.set(target, current);
        for (int i = 0; i < ordered.size(); i++) {
            sortSetter.accept(ordered.get(i), i + 1);
        }
        return true;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private Map<Long, Long> countByFirstColumn(String sql) {
        Map<Long, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getLong(1), rs.getLong(2));
        });
        return result;
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeThemeColor(String value) {
        String color = normalizeRequired(value);
        return color.isEmpty() ? DEFAULT_THEME_COLOR : color.toUpperCase(Locale.ROOT);
    }

    private String stageLabel(Subject subject, Stage stage) {
        return subject.getName() + " / " + stage.getName();
    }

    private String buildSubjectDeleteMessage(long stageCount, long questionCount,
                                             long courseCount, long routeCount) {
        StringBuilder message = new StringBuilder("该学科存在关联内容，无法删除：");
        if (stageCount > 0) {
            message.append(stageCount).append(" 个阶段、");
        }
        if (questionCount > 0) {
            message.append(questionCount).append(" 道题目、");
        }
        if (courseCount > 0) {
            message.append(courseCount).append(" 门课程、");
        }
        if (routeCount > 0) {
            message.append(routeCount).append(" 条学习路线、");
        }
        message.setLength(message.length() - 1);
        return message.append("，请先处理关联内容").toString();
    }

    private void audit(Actor actor, String action, String targetType, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action,
                targetType, targetName, actor.ip());
    }
}
