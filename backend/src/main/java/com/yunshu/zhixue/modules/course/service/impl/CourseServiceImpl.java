package com.yunshu.zhixue.modules.course.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportExcelRow;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewVO;
import com.yunshu.zhixue.modules.course.dto.CourseImportResultVO;
import com.yunshu.zhixue.modules.course.dto.CourseStatsVO;
import com.yunshu.zhixue.modules.course.dto.CourseVO;
import com.yunshu.zhixue.modules.course.dto.CreateCourseRequest;
import com.yunshu.zhixue.modules.course.dto.UpdateCourseRequest;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * B 站课程库实现。课程与额外学科的关系表采用覆盖写（与 role_permission 一致），
 * 避免逻辑删除行与 course_subject 唯一键冲突。
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String TARGET_COURSE = "课程";
    private static final String TARGET_SUBJECT = "学科";
    private static final String OTHER_SUBJECT_NAME = "其他学科";
    private static final int SOURCE_BILIBILI = 1;
    private static final int SOURCE_OTHER = 2;
    private static final int MAX_DURATION_MINUTES = 100_000;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_SUBJECT_NAME_LENGTH = 50;
    private static final long MAX_IMPORT_FILE_SIZE = 10L * 1024 * 1024;

    private static final String DEFAULT_SUBJECT_ICON = "📚";
    private static final String DEFAULT_SUBJECT_DESCRIPTION = "批量导入课程时自动创建";
    private static final String DEFAULT_SUBJECT_THEME_COLOR = "#2563EB";

    private static final Pattern BVID_PATTERN = Pattern.compile("(?i)(BV[0-9A-Za-z]{10})");
    private static final Pattern BVID_FORMAT_PATTERN = Pattern.compile("^BV[0-9A-Za-z]{10}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+");
    private static final Pattern SUBJECT_SEPARATOR = Pattern.compile("[,，、]");

    /** 导入表格必需表头（按名称匹配，列顺序不敏感）。 */
    private static final List<String> REQUIRED_HEADERS = List.of(
            "课程名称", "所属学科", "来源", "时长（分钟）", "BV号", "封面", "播放链接");

    private static final String PREVIEW_KEY_PREFIX = "course:import:preview:";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectMapper subjectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 查询 ====================

    @Override
    public CourseStatsVO stats() {
        long totalCourses = count("SELECT COUNT(*) FROM course WHERE delete_flag = 0");
        Long totalMinutes = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(duration_minutes), 0) FROM course WHERE delete_flag = 0",
                Long.class);
        BigDecimal hours = BigDecimal.valueOf(totalMinutes == null ? 0L : totalMinutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        long relatedSubjects = count("""
                SELECT COUNT(DISTINCT t.subject_id)
                FROM (
                    SELECT subject_id FROM course
                    WHERE delete_flag = 0 AND subject_id IS NOT NULL
                    UNION ALL
                    SELECT cs.subject_id FROM course_subject cs
                    JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                    WHERE cs.delete_flag = 0
                ) t
                """);
        return new CourseStatsVO(totalCourses, hours, relatedSubjects);
    }

    @Override
    public PageResult<CourseVO> page(String keyword, Long subjectId, Integer source,
                                     long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<Course> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Course::getName, kw).or().like(Course::getBvid, kw));
        }
        if (source != null) {
            wrapper.eq(Course::getSource, source);
        }
        if (subjectId != null) {
            if (subjectId == 0L) {
                wrapper.isNull(Course::getSubjectId);
            } else {
                // 主学科或额外关联学科命中均可筛选到
                wrapper.apply("(subject_id = {0} OR id IN ("
                        + "SELECT course_id FROM course_subject "
                        + "WHERE delete_flag = 0 AND subject_id = {0}))", subjectId);
            }
        }
        wrapper.orderByDesc(Course::getId);

        IPage<Course> result = courseMapper.selectPage(new Page<>(pageNo, size), wrapper);
        List<CourseVO> list = toVOs(result.getRecords());
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private List<CourseVO> toVOs(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        Map<Long, List<Long>> extraSubjectIds = new HashMap<>();
        List<CourseSubject> relations = courseSubjectMapper.selectList(
                Wrappers.lambdaQuery(CourseSubject.class)
                        .in(CourseSubject::getCourseId, courseIds)
                        .orderByAsc(CourseSubject::getId));
        for (CourseSubject relation : relations) {
            extraSubjectIds.computeIfAbsent(relation.getCourseId(), key -> new ArrayList<>())
                    .add(relation.getSubjectId());
        }

        Set<Long> allSubjectIds = new LinkedHashSet<>();
        courses.stream().map(Course::getSubjectId).filter(Objects::nonNull)
                .forEach(allSubjectIds::add);
        extraSubjectIds.values().forEach(allSubjectIds::addAll);
        Map<Long, String> subjectNames = allSubjectIds.isEmpty()
                ? Map.of()
                : subjectMapper.selectBatchIds(allSubjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, Subject::getName,
                                (left, right) -> left));

        return courses.stream().map(course -> {
            List<Long> ids = extraSubjectIds.getOrDefault(course.getId(), List.of());
            List<String> names = ids.stream()
                    .map(id -> subjectNames.getOrDefault(id, "未知学科"))
                    .toList();
            String mainName = course.getSubjectId() == null
                    ? OTHER_SUBJECT_NAME
                    : subjectNames.getOrDefault(course.getSubjectId(), "未知学科");
            return new CourseVO(
                    course.getId(),
                    course.getName(),
                    course.getCoverUrl(),
                    course.getDurationMinutes(),
                    course.getSource(),
                    course.getLink(),
                    course.getBvid(),
                    course.getSubjectId(),
                    mainName,
                    ids,
                    names);
        }).toList();
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional
    public void create(CreateCourseRequest request, Actor actor) {
        Course course = new Course();
        applyForm(course, request.name(), request.coverUrl(), request.durationMinutes(),
                request.source(), request.link(), request.subjectId());
        List<Long> extraIds = normalizeExtraSubjectIds(request.subjectIds(), course.getSubjectId());

        try {
            courseMapper.insert(course);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "该课程链接对应的 BV 号已存在");
        }
        replaceExtraSubjects(course.getId(), extraIds);
        audit(actor, "新增", course.getName());
    }

    @Override
    @Transactional
    public void update(Long id, UpdateCourseRequest request, Actor actor) {
        Course course = requireCourse(id);
        applyForm(course, request.name(), request.coverUrl(), request.durationMinutes(),
                request.source(), request.link(), request.subjectId());
        List<Long> extraIds = normalizeExtraSubjectIds(request.subjectIds(), course.getSubjectId());

        try {
            // updateById 默认忽略 null，这里显式 set 以支持清空封面/链接/学科
            courseMapper.update(null, Wrappers.lambdaUpdate(Course.class)
                    .eq(Course::getId, id)
                    .set(Course::getName, course.getName())
                    .set(Course::getCoverUrl, course.getCoverUrl())
                    .set(Course::getDurationMinutes, course.getDurationMinutes())
                    .set(Course::getSource, course.getSource())
                    .set(Course::getLink, course.getLink())
                    .set(Course::getBvid, course.getBvid())
                    .set(Course::getSubjectId, course.getSubjectId()));
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "该课程链接对应的 BV 号已存在");
        }
        replaceExtraSubjects(course.getId(), extraIds);
        audit(actor, "修改", course.getName());
    }

    /**
     * 校验并填充课程公共字段。主学科传 null / 0 表示「其他学科」。
     */
    private void applyForm(Course course, String name, String coverUrl, Integer durationMinutes,
                           Integer source, String link, Long subjectId) {
        String courseName = name.trim();
        Long mainSubjectId = subjectId == null || subjectId == 0 ? null : subjectId;
        if (mainSubjectId != null) {
            requireSubject(mainSubjectId);
        }
        String normalizedLink = normalizeOptional(link);
        String bvid = source == SOURCE_BILIBILI ? parseBvid(normalizedLink) : null;
        assertBvidAvailable(bvid, course.getId());

        course.setName(courseName);
        course.setCoverUrl(normalizeOptional(coverUrl));
        course.setDurationMinutes(durationMinutes);
        course.setSource(source);
        course.setLink(normalizedLink);
        course.setBvid(bvid);
        course.setSubjectId(mainSubjectId);
    }

    @Override
    @Transactional
    public void delete(Long id, Actor actor) {
        Course course = requireCourse(id);
        long routeCount = count("""
                SELECT COUNT(DISTINCT rc.route_id)
                FROM route_course rc
                JOIN learning_route lr ON lr.id = rc.route_id AND lr.delete_flag = 0
                WHERE rc.delete_flag = 0 AND rc.course_id = ?
                """, id);
        if (routeCount > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该课程被 " + routeCount + " 条学习路线引用，请先从路线中移除后再删除");
        }

        courseMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM course_subject WHERE course_id = ?", id);
        audit(actor, "删除", course.getName());
    }

    // ==================== 批量导入 ====================

    @Override
    public CourseImportPreviewVO previewImport(MultipartFile file, Actor actor) {
        validateImportFile(file);
        List<CourseImportExcelRow> excelRows = readExcelRows(file);
        if (excelRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "表格中没有可导入的数据");
        }

        Map<String, Long> subjectsByName = subjectMapper.selectList(
                        Wrappers.lambdaQuery(Subject.class))
                .stream()
                .collect(Collectors.toMap(Subject::getName, Subject::getId, (left, right) -> left));

        List<CourseImportPreviewVO.Row> rows = new ArrayList<>();
        Map<String, Integer> bvidRows = new HashMap<>();
        for (CourseImportExcelRow excelRow : excelRows) {
            rows.add(parseExcelRow(excelRow, subjectsByName, bvidRows));
        }

        int validCount = 0;
        int warningCount = 0;
        int invalidCount = 0;
        for (CourseImportPreviewVO.Row row : rows) {
            switch (row.status()) {
                case "valid" -> validCount++;
                case "warning" -> warningCount++;
                default -> invalidCount++;
            }
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String key = previewKey(actor.userId(), token);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(rows), PREVIEW_TTL);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
        }
        return new CourseImportPreviewVO(token, rows.size(), validCount, warningCount,
                invalidCount, rows);
    }

    private CourseImportPreviewVO.Row parseExcelRow(CourseImportExcelRow data,
                                                    Map<String, Long> subjectsByName,
                                                    Map<String, Integer> bvidRows) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int lineNo = data.getRowNo() == null ? 0 : data.getRowNo();
        String name = trim(data.getName());
        String subjectRaw = trim(data.getSubject());
        String sourceRaw = trim(data.getSource());
        String durationRaw = trim(data.getDurationMinutes());
        String bvidRaw = trim(data.getBvid());
        String coverUrl = trim(data.getCoverUrl());
        String link = trim(data.getLink());

        if (name.isEmpty()) {
            errors.add("课程名称不能为空");
        } else if (name.length() > MAX_NAME_LENGTH) {
            errors.add("课程名称不能超过 " + MAX_NAME_LENGTH + " 位");
        }

        Integer durationMinutes = null;
        if (durationRaw.isEmpty()) {
            errors.add("时长不能为空");
        } else {
            try {
                durationMinutes = Integer.parseInt(durationRaw);
                if (durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES) {
                    errors.add("时长需为 1 ~ " + MAX_DURATION_MINUTES + " 的整数（分钟）");
                }
            } catch (NumberFormatException ex) {
                errors.add("时长需为整数（分钟）");
            }
        }

        Integer source = parseSource(sourceRaw);
        if (source == null) {
            errors.add("来源需为「B站」或「其他」");
        }

        if (!link.isEmpty()) {
            if (link.length() > MAX_URL_LENGTH) {
                errors.add("课程链接不能超过 " + MAX_URL_LENGTH + " 位");
            } else if (!URL_PATTERN.matcher(link).matches()) {
                errors.add("课程链接需以 http:// 或 https:// 开头");
            }
        }
        if (!coverUrl.isEmpty()) {
            if (coverUrl.length() > MAX_URL_LENGTH) {
                errors.add("封面 URL 不能超过 " + MAX_URL_LENGTH + " 位");
            } else if (!URL_PATTERN.matcher(coverUrl).matches()) {
                errors.add("封面 URL 需以 http:// 或 https:// 开头");
            }
        }

        // 多学科：第一个为主学科，其余为关联学科
        List<String> subjectNames = splitSubjects(subjectRaw);
        String mainSubjectName = subjectNames.isEmpty() ? "" : subjectNames.get(0);
        Long mainSubjectId = mainSubjectName.isEmpty() ? null : subjectsByName.get(mainSubjectName);
        if (subjectNames.isEmpty()) {
            warnings.add("未填写学科，将归入其他学科");
        }
        for (String subjectName : subjectNames) {
            if (subjectName.length() > MAX_SUBJECT_NAME_LENGTH) {
                errors.add("学科名称「" + subjectName + "」不能超过 " + MAX_SUBJECT_NAME_LENGTH + " 位");
            } else if (!subjectsByName.containsKey(subjectName)) {
                warnings.add("学科「" + subjectName + "」不存在，导入时将自动创建");
            }
        }
        List<String> extraSubjectNames = subjectNames.size() > 1
                ? List.copyOf(subjectNames.subList(1, subjectNames.size()))
                : List.of();

        // BV 号优先取表格列，缺失时从播放链接解析
        String bvid = null;
        if (source != null && source == SOURCE_BILIBILI) {
            if (!bvidRaw.isEmpty()) {
                if (!BVID_FORMAT_PATTERN.matcher(bvidRaw).matches()) {
                    errors.add("BV 号格式应为 BV 开头加 10 位字母数字");
                } else {
                    bvid = bvidRaw;
                }
            } else {
                bvid = parseBvid(link);
            }
        }
        if (bvid != null) {
            Integer firstRow = bvidRows.get(bvid);
            if (firstRow != null) {
                errors.add("BV 号 " + bvid + " 与第 " + firstRow + " 行重复");
            } else if (bvidExists(bvid)) {
                errors.add("BV 号 " + bvid + " 已存在课程库");
            } else {
                bvidRows.put(bvid, lineNo);
            }
        }

        List<String> messages = new ArrayList<>(errors);
        messages.addAll(warnings);
        String status = !errors.isEmpty() ? "invalid" : !warnings.isEmpty() ? "warning" : "valid";
        return new CourseImportPreviewVO.Row(
                lineNo,
                name,
                durationMinutes,
                mainSubjectName,
                mainSubjectId,
                extraSubjectNames,
                source,
                normalizeOptional(link),
                normalizeOptional(coverUrl),
                bvid,
                status,
                List.copyOf(messages));
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "请选择要导入的表格文件");
        }
        String filename = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 .xlsx / .xls 表格文件");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_INVALID, "文件大小不能超过 10MB");
        }
    }

    private List<CourseImportExcelRow> readExcelRows(MultipartFile file) {
        List<CourseImportExcelRow> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, CourseImportExcelRow.class,
                    new AnalysisEventListener<CourseImportExcelRow>() {
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap,
                                                  AnalysisContext context) {
                            Set<String> heads = headMap.values().stream()
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .collect(Collectors.toSet());
                            List<String> missing = REQUIRED_HEADERS.stream()
                                    .filter(header -> !heads.contains(header))
                                    .toList();
                            if (!missing.isEmpty()) {
                                throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                                        "表格表头缺少列：" + String.join("、", missing));
                            }
                        }

                        @Override
                        public void invoke(CourseImportExcelRow data, AnalysisContext context) {
                            if (rows.size() >= AppConstants.MAX_IMPORT_ROWS) {
                                throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                                        "单次最多导入 " + AppConstants.MAX_IMPORT_ROWS + " 行");
                            }
                            data.setRowNo(context.readRowHolder().getRowIndex() + 1);
                            rows.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 无后续处理
                        }
                    }).sheet().headRowNumber(1).doRead();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            BizException bizException = findBizException(ex);
            if (bizException != null) {
                throw bizException;
            }
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "表格解析失败，请确认文件为 .xlsx/.xls 且表头格式正确");
        }
        return rows;
    }

    private BizException findBizException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BizException bizException) {
                return bizException;
            }
            current = current.getCause();
        }
        return null;
    }

    @Override
    @Transactional
    public CourseImportResultVO confirmImport(String token, Actor actor) {
        String key = previewKey(actor.userId(), token.trim());
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新解析");
        }

        List<CourseImportPreviewVO.Row> rows;
        try {
            rows = objectMapper.readValue(json,
                    new TypeReference<List<CourseImportPreviewVO.Row>>() {
                    });
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览解析失败，请重新解析");
        }

        List<CourseImportPreviewVO.Row> importable = rows.stream()
                .filter(row -> !"invalid".equals(row.status()))
                .toList();
        if (importable.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "没有可导入的课程");
        }

        Map<String, Long> subjectCache = subjectMapper.selectList(
                        Wrappers.lambdaQuery(Subject.class))
                .stream()
                .collect(Collectors.toMap(Subject::getName, Subject::getId, (left, right) -> left));

        long imported = 0;
        for (CourseImportPreviewVO.Row row : importable) {
            Long mainSubjectId = ensureSubjectId(row.subjectName(), subjectCache, actor);
            LinkedHashSet<Long> extraIds = new LinkedHashSet<>();
            for (String extraName : row.extraSubjectNames()) {
                Long extraId = ensureSubjectId(extraName, subjectCache, actor);
                if (extraId != null && !extraId.equals(mainSubjectId)) {
                    extraIds.add(extraId);
                }
            }

            Course course = new Course();
            course.setName(row.name());
            course.setCoverUrl(row.coverUrl());
            course.setDurationMinutes(row.durationMinutes());
            course.setSource(row.source());
            course.setLink(row.link());
            course.setBvid(row.bvid());
            course.setSubjectId(mainSubjectId);
            try {
                courseMapper.insert(course);
            } catch (DuplicateKeyException ex) {
                throw new BizException(ErrorCode.CONFLICT,
                        "第 " + row.row() + " 行 BV 号已存在，导入已取消");
            }
            replaceExtraSubjects(course.getId(), new ArrayList<>(extraIds));
            imported++;
        }

        redisTemplate.delete(key);
        audit(actor, "新增", "批量导入 " + imported + " 门课程");
        return new CourseImportResultVO(imported);
    }

    // ==================== 内部工具 ====================

    private Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private void requireSubject(Long id) {
        if (subjectMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
    }

    /**
     * 按名称获取学科 ID；不存在时自动创建（批量导入场景）。
     */
    private Long ensureSubjectId(String name, Map<String, Long> subjectCache, Actor actor) {
        String subjectName = trim(name);
        if (subjectName.isEmpty()) {
            return null;
        }
        Long cached = subjectCache.get(subjectName);
        if (cached != null) {
            return cached;
        }

        Subject subject = new Subject();
        subject.setName(subjectName);
        subject.setIcon(DEFAULT_SUBJECT_ICON);
        subject.setDescription(DEFAULT_SUBJECT_DESCRIPTION);
        subject.setThemeColor(DEFAULT_SUBJECT_THEME_COLOR);
        subject.setSortOrder(nextSubjectSortOrder());
        try {
            subjectMapper.insert(subject);
        } catch (DuplicateKeyException ex) {
            Subject created = subjectMapper.selectOne(
                    Wrappers.lambdaQuery(Subject.class)
                            .eq(Subject::getName, subjectName)
                            .last("LIMIT 1"));
            if (created == null) {
                throw new BizException(ErrorCode.CONFLICT, "学科「" + subjectName + "」自动创建失败");
            }
            subjectCache.put(subjectName, created.getId());
            return created.getId();
        }
        subjectCache.put(subjectName, subject.getId());
        operationLogService.record(actor.userId(), actor.username(), "新增", TARGET_SUBJECT,
                subjectName + "（批量导入自动创建）", actor.ip());
        return subject.getId();
    }

    private List<String> splitSubjects(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String part : SUBJECT_SEPARATOR.split(raw)) {
            String name = part.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private int nextSubjectSortOrder() {
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM subject WHERE delete_flag = 0",
                Long.class);
        return Math.toIntExact((max == null ? 0L : max) + 1);
    }

    private List<Long> normalizeExtraSubjectIds(List<Long> subjectIds, Long mainSubjectId) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = subjectIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (mainSubjectId != null) {
            ids.remove(mainSubjectId);
        }
        ids.forEach(this::requireSubject);
        return new ArrayList<>(ids);
    }

    /**
     * 关联表覆盖写：先物理清理再插入，避免逻辑删除行占用唯一键。
     */
    private void replaceExtraSubjects(Long courseId, List<Long> subjectIds) {
        jdbcTemplate.update("DELETE FROM course_subject WHERE course_id = ?", courseId);
        for (Long subjectId : subjectIds) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(courseId);
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private String parseBvid(String link) {
        if (link == null) {
            return null;
        }
        Matcher matcher = BVID_PATTERN.matcher(link);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 唯一键对逻辑删除行同样生效，预检包含已删除课程，避免插入时直接报冲突。
     */
    private void assertBvidAvailable(String bvid, Long excludeCourseId) {
        if (bvid == null) {
            return;
        }
        long count = excludeCourseId == null
                ? count("SELECT COUNT(*) FROM course WHERE bvid = ?", bvid)
                : count("SELECT COUNT(*) FROM course WHERE bvid = ? AND id <> ?",
                        bvid, excludeCourseId);
        if (count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该课程链接对应的 BV 号已存在");
        }
    }

    private boolean bvidExists(String bvid) {
        return count("SELECT COUNT(*) FROM course WHERE bvid = ?", bvid) > 0;
    }

    private Integer parseSource(String raw) {
        String value = raw.trim();
        if ("B站".equalsIgnoreCase(value) || "b站".equalsIgnoreCase(value)
                || "1".equals(value) || "bilibili".equalsIgnoreCase(value)) {
            return SOURCE_BILIBILI;
        }
        if ("其他".equals(value) || "2".equals(value)) {
            return SOURCE_OTHER;
        }
        return null;
    }

    private String previewKey(Long userId, String token) {
        return PREVIEW_KEY_PREFIX + userId + ":" + token;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action,
                TARGET_COURSE, targetName, actor.ip());
    }
}
