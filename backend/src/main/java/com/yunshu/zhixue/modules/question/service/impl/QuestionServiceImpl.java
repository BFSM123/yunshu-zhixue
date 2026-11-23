package com.yunshu.zhixue.modules.question.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.BatchAuditRequest;
import com.yunshu.zhixue.modules.question.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.question.dto.BatchQuestionStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionDetailVO;
import com.yunshu.zhixue.modules.question.dto.QuestionExportRow;
import com.yunshu.zhixue.modules.question.dto.QuestionImportExcelRow;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportResultVO;
import com.yunshu.zhixue.modules.question.dto.QuestionImportTokenPayload;
import com.yunshu.zhixue.modules.question.dto.QuestionListItemVO;
import com.yunshu.zhixue.modules.question.dto.QuestionOptionVO;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsVO;
import com.yunshu.zhixue.modules.question.dto.QuestionStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionTemplateRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 题库管理实现。
 *
 * <p>选项为题目子记录，采用物理覆盖写（先删后插）避免逻辑删除行占用
 * uk_question_option(question_id, option_key) 唯一键；题目本身仍走逻辑删除。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String TARGET_QUESTION = "题目";
    private static final String TARGET_STAGE = "阶段";
    private static final String TARGET_SUBJECT = "学科";

    private static final String DEFAULT_SUBJECT_ICON = "📚";
    private static final String DEFAULT_SUBJECT_DESCRIPTION = "批量导入题目时自动创建";
    private static final String DEFAULT_SUBJECT_THEME_COLOR = "#2563EB";

    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_MULTIPLE = 2;
    private static final int TYPE_JUDGMENT = 3;
    private static final int TYPE_SHORT_ANSWER = 4;

    private static final int DIFFICULTY_EASY = 1;
    private static final int DIFFICULTY_MEDIUM = 2;
    private static final int DIFFICULTY_HARD = 3;

    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 2;
    private static final int STATUS_PENDING = 3;
    private static final int STATUS_REJECTED = 5;

    private static final int MAX_OPTION_COUNT = 6;
    private static final int MAX_OPTION_TEXT_LENGTH = 500;
    private static final int MAX_TAGS_LENGTH = 255;
    private static final int MAX_ANALYSIS_LENGTH = 10_000;
    private static final int MAX_SUBJECT_NAME_LENGTH = 50;
    private static final int MAX_STAGE_NAME_LENGTH = 50;
    private static final long MAX_IMPORT_FILE_SIZE = 10L * 1024 * 1024;

    private static final List<String> OPTION_KEYS = List.of("A", "B", "C", "D", "E", "F");
    private static final Pattern ANSWER_SEPARATOR = Pattern.compile("[,，、;；\\s]+");

    private static final String PREVIEW_KEY_PREFIX = "question:import:preview:";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private static final List<String> REQUIRED_HEADERS = List.of("学科", "阶段", "难度", "正确答案");

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectMapper subjectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 查询 ====================

    @Override
    public QuestionStatsVO stats() {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(*)                                                   AS total,
                       COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0)   AS on_shelf,
                       COALESCE(SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END), 0)   AS pending,
                       COALESCE(SUM(CASE WHEN type = 1 THEN 1 ELSE 0 END), 0)     AS single_count,
                       COALESCE(SUM(CASE WHEN type = 2 THEN 1 ELSE 0 END), 0)     AS multiple_count,
                       COALESCE(SUM(CASE WHEN type = 3 THEN 1 ELSE 0 END), 0)     AS judgment_count,
                       COALESCE(SUM(CASE WHEN type = 4 THEN 1 ELSE 0 END), 0)     AS short_answer_count
                FROM question
                WHERE delete_flag = 0
                """);
        return new QuestionStatsVO(
                longValue(row.get("total")),
                longValue(row.get("on_shelf")),
                longValue(row.get("pending")),
                longValue(row.get("single_count")),
                longValue(row.get("multiple_count")),
                longValue(row.get("judgment_count")),
                longValue(row.get("short_answer_count")));
    }

    @Override
    public PageResult<QuestionListItemVO> page(String keyword, Long subjectId, Long stageId,
                                               Integer type, Integer difficulty, Integer status,
                                               long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<Question> wrapper = buildQuery(keyword, subjectId, stageId,
                type, difficulty, status);
        wrapper.orderByDesc(Question::getId);

        IPage<Question> result = questionMapper.selectPage(new Page<>(pageNo, size), wrapper);
        return PageResult.of(toListVOs(result.getRecords()), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public QuestionDetailVO detail(Long id) {
        Question question = requireQuestion(id);
        Stage stage = question.getStageId() == null ? null : stageMapper.selectById(question.getStageId());
        Subject subject = stage == null ? null : subjectMapper.selectById(stage.getSubjectId());
        List<QuestionOption> options = optionsOf(id);

        List<QuestionOptionVO> optionVOs = options.stream()
                .map(option -> new QuestionOptionVO(option.getId(), option.getOptionKey(),
                        option.getOptionText(), option.getIsCorrect(), option.getSortOrder()))
                .toList();

        String answer;
        if (isChoice(question.getType())) {
            answer = options.stream()
                    .filter(option -> Objects.equals(option.getIsCorrect(), 1))
                    .map(QuestionOption::getOptionKey)
                    .collect(Collectors.joining(","));
        } else {
            answer = question.getAnswer();
        }

        Map<String, Object> stat = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS done_count,
                       COALESCE(SUM(is_correct), 0) AS correct_count
                FROM practice_record
                WHERE delete_flag = 0 AND question_id = ?
                """, id);
        long doneCount = longValue(stat.get("done_count"));
        long correctCount = longValue(stat.get("correct_count"));
        BigDecimal accuracyRate = doneCount == 0
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(correctCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(doneCount), 1, RoundingMode.HALF_UP);

        return new QuestionDetailVO(
                question.getId(),
                question.getContent(),
                question.getStageId(),
                stage == null ? null : stage.getName(),
                stage == null ? null : stage.getSubjectId(),
                subject == null ? null : subject.getName(),
                question.getType(),
                question.getDifficulty(),
                question.getStatus(),
                question.getRejectReason(),
                question.getTags(),
                answer,
                question.getAnalysis(),
                optionVOs,
                doneCount,
                correctCount,
                accuracyRate,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private List<QuestionListItemVO> toListVOs(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Set<Long> stageIds = questions.stream()
                .map(Question::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Stage> stages = stageIds.isEmpty()
                ? Map.of()
                : stageMapper.selectBatchIds(stageIds).stream()
                        .collect(Collectors.toMap(Stage::getId, stage -> stage, (left, right) -> left));

        Set<Long> subjectIds = stages.values().stream()
                .map(Stage::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Subject> subjects = subjectIds.isEmpty()
                ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, subject -> subject, (left, right) -> left));

        return questions.stream().map(question -> {
            Stage stage = stages.get(question.getStageId());
            Subject subject = stage == null ? null : subjects.get(stage.getSubjectId());
            return new QuestionListItemVO(
                    question.getId(),
                    question.getContent(),
                    question.getStageId(),
                    stage == null ? null : stage.getName(),
                    stage == null ? null : stage.getSubjectId(),
                    subject == null ? null : subject.getName(),
                    question.getType(),
                    question.getDifficulty(),
                    question.getStatus(),
                    question.getTags(),
                    question.getCreatedAt());
        }).toList();
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional
    public void create(QuestionSaveRequest request, Actor actor) {
        requireStage(request.stageId());
        List<OptionValue> options = normalizeOptions(request);
        String answer = normalizeAnswer(request);

        Question question = new Question();
        question.setStageId(request.stageId());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setContent(request.content().trim());
        question.setAnalysis(normalizeOptional(request.analysis()));
        question.setTags(normalizeTags(request.tags()));
        question.setAnswer(answer);
        question.setStatus(STATUS_PENDING);
        questionMapper.insert(question);

        insertOptions(question.getId(), options);
        audit(actor, "新增", abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void update(Long id, QuestionSaveRequest request, Actor actor) {
        Question existing = requireQuestion(id);
        requireStage(request.stageId());
        List<OptionValue> options = normalizeOptions(request);
        String answer = normalizeAnswer(request);

        // 编辑后状态保持（Q2）：待审核/已驳回统一回到待审核；已上架/已下架保持原状态。
        int newStatus = existing.getStatus();
        String newRejectReason = existing.getRejectReason();
        if (newStatus == STATUS_PENDING || newStatus == STATUS_REJECTED) {
            newStatus = STATUS_PENDING;
            newRejectReason = null;
        }

        String content = request.content().trim();
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .eq(Question::getId, id)
                .set(Question::getStageId, request.stageId())
                .set(Question::getType, request.type())
                .set(Question::getDifficulty, request.difficulty())
                .set(Question::getContent, content)
                .set(Question::getAnalysis, normalizeOptional(request.analysis()))
                .set(Question::getTags, normalizeTags(request.tags()))
                .set(Question::getAnswer, answer)
                .set(Question::getStatus, newStatus)
                .set(Question::getRejectReason, newRejectReason));

        replaceOptions(id, options);
        audit(actor, "修改", abbreviate(content));
    }

    @Override
    @Transactional
    public void delete(Long id, Actor actor) {
        Question question = requireQuestion(id);
        long referenced = countReferences(List.of(id));
        if (referenced > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该题目已有练习或错题记录，不可删除");
        }
        questionMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM question_option WHERE question_id = ?", id);
        audit(actor, "删除", abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void audit(Long id, QuestionAuditRequest request, Actor actor) {
        Question question = requireQuestion(id);
        if (!Objects.equals(question.getStatus(), STATUS_PENDING)) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION, "仅待审核题目可执行审核");
        }
        applyAudit(question, request.pass(), request.rejectReason());
        audit(actor, Boolean.TRUE.equals(request.pass()) ? "审核通过" : "审核驳回",
                abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, QuestionStatusRequest request, Actor actor) {
        Question question = requireQuestion(id);
        int target = transitionStatus(question, request.action());
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .eq(Question::getId, id)
                .set(Question::getStatus, target));
        audit(actor, "on".equals(request.action()) ? "提交审核" : "下架",
                abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void resubmit(Long id, Actor actor) {
        Question question = requireQuestion(id);
        if (!Objects.equals(question.getStatus(), STATUS_REJECTED)) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION, "仅已驳回题目可重新提交");
        }
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .eq(Question::getId, id)
                .set(Question::getStatus, STATUS_PENDING)
                .set(Question::getRejectReason, null));
        audit(actor, "重新提交", abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public int batchUpdateStatus(BatchQuestionStatusRequest request, Actor actor) {
        List<Question> questions = questionMapper.selectBatchIds(request.ids());
        List<Long> targetIds = questions.stream()
                .filter(question -> canTransition(question, request.action()))
                .map(Question::getId)
                .toList();
        if (targetIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "所选题目当前状态不支持该操作");
        }
        int target = "on".equals(request.action()) ? STATUS_PENDING : STATUS_OFF;
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .in(Question::getId, targetIds)
                .set(Question::getStatus, target));
        audit(actor, "on".equals(request.action()) ? "批量提交审核" : "批量下架",
                "共 " + targetIds.size() + " 道题目");
        return targetIds.size();
    }

    @Override
    @Transactional
    public int batchDelete(BatchIdsRequest request, Actor actor) {
        List<Question> questions = questionMapper.selectBatchIds(request.ids());
        if (questions.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "所选题目不存在");
        }
        List<Long> ids = questions.stream().map(Question::getId).toList();
        long referenced = countReferences(ids);
        if (referenced > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "所选题目中有 " + referenced + " 条练习或错题记录，不可删除");
        }
        questionMapper.deleteBatchIds(ids);
        ids.forEach(id -> jdbcTemplate.update(
                "DELETE FROM question_option WHERE question_id = ?", id));
        audit(actor, "批量删除", "共 " + ids.size() + " 道题目");
        return ids.size();
    }

    @Override
    @Transactional
    public int batchAudit(BatchAuditRequest request, Actor actor) {
        boolean pass = Boolean.TRUE.equals(request.pass());
        String reason = pass ? null : normalizeOptional(request.rejectReason());
        if (!pass && reason == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "驳回时必须填写驳回原因");
        }
        List<Question> questions = questionMapper.selectBatchIds(request.ids());
        List<Long> pendingIds = questions.stream()
                .filter(question -> Objects.equals(question.getStatus(), STATUS_PENDING))
                .map(Question::getId)
                .toList();
        if (pendingIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "所选题目中没有待审核题目");
        }
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .in(Question::getId, pendingIds)
                .set(Question::getStatus, pass ? STATUS_ON : STATUS_REJECTED)
                .set(Question::getRejectReason, reason));
        audit(actor, pass ? "批量审核通过" : "批量审核驳回",
                "共 " + pendingIds.size() + " 道题目");
        return pendingIds.size();
    }

    // ==================== Excel 导入 ====================

    @Override
    public QuestionImportPreviewVO previewImport(MultipartFile file, Actor actor) {
        validateImportFile(file);

        List<QuestionImportExcelRow> excelRows = readExcelRows(file);
        if (excelRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "表格中没有可导入的数据");
        }

        Map<String, Long> subjectIdsByName = loadSubjectIds();
        Map<String, Long> stageIdsByKey = loadStageIdsBySubject();

        // 只为文件中已存在的学科加载题库判重数据，避免全表扫描。
        Set<Long> existingSubjectIds = new HashSet<>();
        for (QuestionImportExcelRow excelRow : excelRows) {
            Long subjectId = subjectIdsByName.get(trim(excelRow.getSubject()));
            if (subjectId != null) {
                existingSubjectIds.add(subjectId);
            }
        }
        Set<String> existingFingerprints = loadExistingFingerprints(existingSubjectIds);

        List<QuestionImportPreviewVO.Row> rows = new ArrayList<>();
        Map<String, Integer> seenRows = new HashMap<>();
        Set<String> autoCreateSubjects = new LinkedHashSet<>();
        Set<String> autoCreateStages = new LinkedHashSet<>();

        for (QuestionImportExcelRow excelRow : excelRows) {
            QuestionImportPreviewVO.Row row = parseRow(excelRow, subjectIdsByName,
                    stageIdsByKey, existingFingerprints, seenRows);
            rows.add(row);
            if ("invalid".equals(row.status())) {
                continue;
            }
            if (row.subjectAutoCreate() && row.subjectName() != null
                    && !row.subjectName().isBlank()) {
                autoCreateSubjects.add(row.subjectName());
            }
            if (row.stageAutoCreate() && row.stageName() != null
                    && !row.stageName().isBlank()) {
                autoCreateStages.add(row.subjectName() + "\u0000" + row.stageName());
            }
        }

        int validCount = 0;
        int warningCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;
        for (QuestionImportPreviewVO.Row row : rows) {
            switch (row.status()) {
                case "valid" -> validCount++;
                case "warning" -> warningCount++;
                default -> invalidCount++;
            }
            if (row.duplicate()) {
                duplicateCount++;
            }
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String key = previewKey(actor.userId(), token);
        try {
            QuestionImportTokenPayload payload = new QuestionImportTokenPayload(rows);
            redisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(payload), PREVIEW_TTL);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
        }

        return new QuestionImportPreviewVO(token, rows.size(), validCount, warningCount,
                invalidCount, duplicateCount, autoCreateSubjects.size(),
                autoCreateStages.size(), rows);
    }

    @Override
    @Transactional
    public QuestionImportResultVO confirmImport(String token, Actor actor) {
        String key = previewKey(actor.userId(), token.trim());
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新解析");
        }

        QuestionImportTokenPayload payload;
        try {
            payload = objectMapper.readValue(json, QuestionImportTokenPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览解析失败，请重新解析");
        }

        List<QuestionImportPreviewVO.Row> validRows = payload.rows().stream()
                .filter(row -> !"invalid".equals(row.status()))
                .toList();
        if (validRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "没有可导入的题目");
        }

        List<QuestionImportPreviewVO.Row> importable = validRows.stream()
                .filter(row -> !row.duplicate())
                .toList();
        int skippedCount = validRows.size() - importable.size();

        Map<String, Long> subjectIdsByName = loadSubjectIds();
        Map<String, Long> stageIdsByKey = loadStageIdsBySubject();
        int createdSubjectCount = 0;
        int createdStageCount = 0;

        // 全部为重复题目时不建学科/阶段，直接返回跳过结果。
        if (importable.isEmpty()) {
            redisTemplate.delete(key);
            return new QuestionImportResultVO(0, skippedCount, 0, 0);
        }

        List<Object[]> optionParams = new ArrayList<>();
        long imported = 0;
        for (QuestionImportPreviewVO.Row row : importable) {
            Long subjectId = subjectIdsByName.get(row.subjectName());
            if (subjectId == null) {
                subjectId = ensureSubject(row.subjectName(), actor);
                subjectIdsByName.put(row.subjectName(), subjectId);
                createdSubjectCount++;
            }

            String stageMapKey = stageKey(subjectId, row.stageName());
            Long stageId = stageIdsByKey.get(stageMapKey);
            if (stageId == null) {
                stageId = ensureStage(subjectId, row.stageName(), actor);
                stageIdsByKey.put(stageMapKey, stageId);
                createdStageCount++;
            }

            Question question = new Question();
            question.setStageId(stageId);
            question.setType(row.type());
            question.setDifficulty(row.difficulty());
            question.setContent(row.content());
            question.setAnalysis(row.analysis());
            question.setTags(row.tags());
            question.setAnswer(isChoice(row.type()) ? null : row.answer());
            question.setStatus(STATUS_PENDING);
            questionMapper.insert(question);

            if (isChoice(row.type()) && row.options() != null) {
                int sort = 0;
                for (QuestionImportPreviewVO.OptionPreview option : row.options()) {
                    optionParams.add(new Object[]{
                            question.getId(), option.optionKey(), option.optionText(),
                            option.correct() ? 1 : 0, sort++
                    });
                }
            }
            imported++;
        }

        if (!optionParams.isEmpty()) {
            jdbcTemplate.batchUpdate("""
                    INSERT INTO question_option
                        (question_id, option_key, option_text, is_correct, sort_order)
                    VALUES (?, ?, ?, ?, ?)
                    """, optionParams);
        }

        redisTemplate.delete(key);
        audit(actor, "新增", "批量导入 " + imported + " 道题目"
                + (skippedCount > 0 ? "（跳过重复 " + skippedCount + " 道）" : ""));
        return new QuestionImportResultVO(imported, skippedCount,
                createdSubjectCount, createdStageCount);
    }

    @Override
    public byte[] template() {
        QuestionTemplateRow single = new QuestionTemplateRow();
        single.setContent("以下哪个是 Java 集合框架中的接口？");
        single.setSubject("AI应用与智能体开发");
        single.setStage("AI工程化-基础与项目实战");
        single.setType("单选");
        single.setDifficulty("简单");
        single.setOptionA("List");
        single.setOptionB("int");
        single.setOptionC("String");
        single.setOptionD("void");
        single.setAnswer("A");
        single.setAnalysis("List 是集合框架接口，其余为基本类型或 final 类。");
        single.setTags("JavaSE基础");

        QuestionTemplateRow multiple = new QuestionTemplateRow();
        multiple.setContent("以下哪些属于关系型数据库？");
        multiple.setSubject("AI应用与智能体开发");
        multiple.setStage("AI工程化-基础与项目实战");
        multiple.setType("多选");
        multiple.setDifficulty("中等");
        multiple.setOptionA("MySQL");
        multiple.setOptionB("Redis");
        multiple.setOptionC("PostgreSQL");
        multiple.setOptionD("MongoDB");
        multiple.setAnswer("A,C");
        multiple.setAnalysis("MySQL 与 PostgreSQL 为关系型数据库。");
        multiple.setTags("数据库");

        QuestionTemplateRow shortAnswer = new QuestionTemplateRow();
        shortAnswer.setContent("简述进程与线程的区别。");
        shortAnswer.setSubject("AI应用与智能体开发");
        shortAnswer.setStage("AI工程化-基础与项目实战");
        shortAnswer.setType("简答");
        shortAnswer.setDifficulty("中等");
        shortAnswer.setAnswer("进程是资源分配的最小单位，线程是 CPU 调度的最小单位；"
                + "同一进程内的线程共享内存与文件句柄。");
        shortAnswer.setAnalysis("围绕资源分配、调度单位、内存共享三点回答即可。");
        shortAnswer.setTags("操作系统");

        return writeExcel(List.of(single, multiple, shortAnswer),
                QuestionTemplateRow.class, "题目数据");
    }

    @Override
    public byte[] export(String keyword, Long subjectId, Long stageId, Integer type,
                         Integer difficulty, Integer status, Actor actor) {
        LambdaQueryWrapper<Question> wrapper = buildQuery(keyword, subjectId, stageId,
                type, difficulty, status);
        wrapper.orderByDesc(Question::getId)
                .last("LIMIT " + (AppConstants.MAX_EXPORT_ROWS + 1));
        List<Question> questions = questionMapper.selectList(wrapper);
        if (questions.size() > AppConstants.MAX_EXPORT_ROWS) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    "导出数据超过 " + AppConstants.MAX_EXPORT_ROWS + " 行，请缩小筛选范围");
        }

        Map<Long, Stage> stages = new HashMap<>();
        Map<Long, Subject> subjects = new HashMap<>();
        Map<Long, List<QuestionOption>> optionsByQuestion = loadOptions(questions);

        Set<Long> stageIds = questions.stream().map(Question::getStageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!stageIds.isEmpty()) {
            stageMapper.selectBatchIds(stageIds).forEach(stage -> stages.put(stage.getId(), stage));
        }
        Set<Long> subjectIds = stages.values().stream().map(Stage::getSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!subjectIds.isEmpty()) {
            subjectMapper.selectBatchIds(subjectIds)
                    .forEach(subject -> subjects.put(subject.getId(), subject));
        }

        List<QuestionExportRow> rows = new ArrayList<>();
        int seq = 1;
        for (Question question : questions) {
            Stage stage = stages.get(question.getStageId());
            Subject subject = stage == null ? null : subjects.get(stage.getSubjectId());
            List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());

            QuestionExportRow row = new QuestionExportRow();
            row.setSeq(seq++);
            row.setContent(question.getContent());
            row.setSubjectName(subject == null ? "" : subject.getName());
            row.setStageName(stage == null ? "" : stage.getName());
            row.setType(typeLabel(question.getType()));
            row.setDifficulty(difficultyLabel(question.getDifficulty()));
            Map<String, String> optionTexts = new LinkedHashMap<>();
            for (QuestionOption option : options) {
                optionTexts.put(option.getOptionKey(), option.getOptionText());
            }
            row.setOptionA(optionTexts.get("A"));
            row.setOptionB(optionTexts.get("B"));
            row.setOptionC(optionTexts.get("C"));
            row.setOptionD(optionTexts.get("D"));
            row.setOptionE(optionTexts.get("E"));
            row.setOptionF(optionTexts.get("F"));
            row.setAnswer(isChoice(question.getType())
                    ? options.stream()
                            .filter(option -> Objects.equals(option.getIsCorrect(), 1))
                            .map(QuestionOption::getOptionKey)
                            .collect(Collectors.joining(","))
                    : question.getAnswer());
            row.setAnalysis(question.getAnalysis());
            row.setTags(question.getTags());
            row.setStatus(statusLabel(question.getStatus()));
            rows.add(row);
        }

        byte[] data = writeExcel(rows, QuestionExportRow.class, "题库");
        audit(actor, "导出", "导出 " + rows.size() + " 道题目");
        return data;
    }

    // ==================== 导入内部逻辑 ====================

    private QuestionImportPreviewVO.Row parseRow(QuestionImportExcelRow data,
                                                 Map<String, Long> subjectIdsByName,
                                                 Map<String, Long> stageIdsByKey,
                                                 Set<String> existingFingerprints,
                                                 Map<String, Integer> seenRows) {
        int lineNo = data.getRowNo() == null ? 0 : data.getRowNo();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String content = firstNonBlank(data.getContent(), data.getContentAlias());
        String subjectName = trim(data.getSubject());
        String stageName = trim(data.getStage());
        String typeRaw = firstNonBlank(data.getType(), data.getTypeAlias());
        String difficultyRaw = trim(data.getDifficulty());
        String rawAnswer = trim(data.getAnswer());
        String analysis = trimToNull(data.getAnalysis());
        String tags = trimToNull(data.getTags());

        if (content.isEmpty()) {
            errors.add("题干不能为空");
        } else if (content.length() > 5000) {
            errors.add("题干不能超过 5000 字");
        }
        if (subjectName.isEmpty()) {
            errors.add("学科不能为空");
        } else if (subjectName.length() > MAX_SUBJECT_NAME_LENGTH) {
            errors.add("学科名称不能超过 " + MAX_SUBJECT_NAME_LENGTH + " 字");
        }
        if (stageName.isEmpty()) {
            errors.add("阶段不能为空");
        } else if (stageName.length() > MAX_STAGE_NAME_LENGTH) {
            errors.add("阶段名称不能超过 " + MAX_STAGE_NAME_LENGTH + " 字");
        }
        if (analysis != null && analysis.length() > MAX_ANALYSIS_LENGTH) {
            errors.add("解析不能超过 " + MAX_ANALYSIS_LENGTH + " 字");
        }
        if (tags != null && tags.length() > MAX_TAGS_LENGTH) {
            errors.add("标签不能超过 " + MAX_TAGS_LENGTH + " 字");
        }

        Integer type = parseType(typeRaw);
        if (type == null) {
            errors.add("题型需为单选/多选/判断/简答");
        }
        Integer difficulty = parseDifficulty(difficultyRaw);
        if (difficulty == null) {
            errors.add("难度需为简单/中等/困难");
        }

        Long subjectId = subjectName.isEmpty() ? null : subjectIdsByName.get(subjectName);
        boolean subjectAutoCreate = !subjectName.isEmpty() && subjectId == null;
        if (subjectAutoCreate) {
            warnings.add("学科「" + subjectName + "」不存在，导入时将自动创建");
        }

        Long stageId = null;
        boolean stageAutoCreate = false;
        if (!subjectName.isEmpty() && !stageName.isEmpty()) {
            stageId = subjectId == null
                    ? null
                    : stageIdsByKey.get(stageKey(subjectId, stageName));
            stageAutoCreate = stageId == null;
            if (stageAutoCreate) {
                warnings.add("阶段「" + stageName + "」不存在，导入时将自动创建");
            }
        }

        List<QuestionImportPreviewVO.OptionPreview> options = List.of();
        String answer = null;

        if (type != null) {
            if (isChoice(type)) {
                Map<String, String> rawOptions = new LinkedHashMap<>();
                putOption(rawOptions, "A", data.getOptionA());
                putOption(rawOptions, "B", data.getOptionB());
                putOption(rawOptions, "C", data.getOptionC());
                putOption(rawOptions, "D", data.getOptionD());
                putOption(rawOptions, "E", data.getOptionE());
                putOption(rawOptions, "F", data.getOptionF());

                if (rawOptions.size() < 2) {
                    errors.add("选择题至少需要 2 个选项");
                }
                for (Map.Entry<String, String> entry : rawOptions.entrySet()) {
                    if (entry.getValue().length() > MAX_OPTION_TEXT_LENGTH) {
                        errors.add("选项" + entry.getKey() + "不能超过 "
                                + MAX_OPTION_TEXT_LENGTH + " 字");
                    }
                }

                List<String> correctKeys = parseAnswerKeys(rawAnswer);
                if (rawAnswer.isEmpty()) {
                    errors.add("正确答案不能为空");
                } else if (correctKeys.isEmpty()) {
                    errors.add("正确答案格式不正确，应为 A 或 A,B,C");
                } else {
                    for (String key : correctKeys) {
                        if (!rawOptions.containsKey(key)) {
                            errors.add("正确答案包含不存在的选项 " + key);
                        }
                    }
                    if (type == TYPE_SINGLE && correctKeys.size() != 1) {
                        errors.add("单选题正确答案必须为 1 个选项");
                    }
                    if (type == TYPE_MULTIPLE && correctKeys.size() < 1) {
                        errors.add("多选题至少需要 1 个正确答案");
                    }
                }

                List<QuestionImportPreviewVO.OptionPreview> previewOptions = new ArrayList<>();
                for (Map.Entry<String, String> entry : rawOptions.entrySet()) {
                    previewOptions.add(new QuestionImportPreviewVO.OptionPreview(
                            entry.getKey(), entry.getValue(), correctKeys.contains(entry.getKey())));
                }
                options = previewOptions;
                answer = correctKeys.isEmpty() ? null : String.join(",", correctKeys);
            } else if (type == TYPE_JUDGMENT) {
                if (rawAnswer.isEmpty()) {
                    errors.add("判断题答案不能为空");
                } else {
                    answer = normalizeJudgment(rawAnswer);
                    if (answer == null) {
                        errors.add("判断题答案需为正确/错误");
                    }
                }
            } else {
                answer = rawAnswer.isEmpty() ? null : rawAnswer;
                if (answer == null) {
                    warnings.add("未填写参考答案");
                }
            }
        }

        boolean duplicate = false;
        if (!content.isEmpty()) {
            // 通用题干（如「下列描述正确的是」）可能对应不同选项，判重需包含答案与选项指纹。
            String dupKey = fingerprint(subjectName, stageName, content, type, answer, options);
            Integer firstRow = seenRows.get(dupKey);
            if (firstRow != null) {
                duplicate = true;
                warnings.add("与第 " + firstRow + " 行题目完全重复，将跳过导入");
            } else {
                seenRows.put(dupKey, lineNo);
            }
            if (stageId != null) {
                String dbKey = questionFingerprint(stageId, content, type, answer, options);
                if (existingFingerprints.contains(dbKey)) {
                    duplicate = true;
                    warnings.add("该阶段下已存在完全相同的题目，将跳过导入");
                }
                // 本行若可导入，也要进入库指纹集合，后续同批相同题目会被跳过。
                if (errors.isEmpty() && !duplicate) {
                    existingFingerprints.add(dbKey);
                }
            }
        }

        List<String> messages = new ArrayList<>(errors);
        messages.addAll(warnings);
        String status = !errors.isEmpty() ? "invalid" : !warnings.isEmpty() ? "warning" : "valid";
        return new QuestionImportPreviewVO.Row(
                lineNo, content, subjectName, subjectId, subjectAutoCreate,
                stageName, stageId, stageAutoCreate, type, difficulty,
                answer, analysis, tags, options, duplicate, status, List.copyOf(messages));
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

    private List<QuestionImportExcelRow> readExcelRows(MultipartFile file) {
        List<QuestionImportExcelRow> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, QuestionImportExcelRow.class,
                    new AnalysisEventListener<QuestionImportExcelRow>() {
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap,
                                                  AnalysisContext context) {
                            Set<String> heads = headMap.values().stream()
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .collect(Collectors.toSet());
                            List<String> missing = new ArrayList<>();
                            if (!heads.contains("题干") && !heads.contains("题目")) {
                                missing.add("题干");
                            }
                            for (String required : REQUIRED_HEADERS) {
                                if (!heads.contains(required)) {
                                    missing.add(required);
                                }
                            }
                            if (!heads.contains("题型") && !heads.contains("类型")) {
                                missing.add("题型");
                            }
                            if (!missing.isEmpty()) {
                                throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                                        "表格表头缺少列：" + String.join("、", missing));
                            }
                        }

                        @Override
                        public void invoke(QuestionImportExcelRow data, AnalysisContext context) {
                            if (rows.size() >= AppConstants.MAX_IMPORT_ROWS) {
                                throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED,
                                        "单次最多导入 " + AppConstants.MAX_IMPORT_ROWS + " 行");
                            }
                            data.setRowNo(context.readRowHolder().getRowIndex() + 1);
                            rows.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 无需后处理
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

    private Map<String, Long> loadSubjectIds() {
        return subjectMapper.selectList(Wrappers.lambdaQuery(Subject.class))
                .stream()
                .collect(Collectors.toMap(Subject::getName, Subject::getId, (left, right) -> left));
    }

    private Map<String, Long> loadStageIdsBySubject() {
        Map<String, Long> result = new HashMap<>();
        for (Stage stage : stageMapper.selectList(Wrappers.lambdaQuery(Stage.class))) {
            result.put(stageKey(stage.getSubjectId(), stage.getName()), stage.getId());
        }
        return result;
    }

    private String stageKey(Long subjectId, String stageName) {
        return subjectId + "\u0000" + stageName;
    }

    /**
     * 加载目标学科下已有题目的完整指纹（阶段 + 题干 + 题型 + 答案 + 选项），
     * 用于识别「完全相同」的重复题目。
     */
    private Set<String> loadExistingFingerprints(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return new HashSet<>();
        }
        String placeholders = subjectIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT q.id, q.stage_id, q.type, q.content, q.answer FROM question q "
                + "JOIN stage s ON s.id = q.stage_id AND s.delete_flag = 0 "
                + "WHERE q.delete_flag = 0 AND s.subject_id IN (" + placeholders + ")";
        List<Question> questions = jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Question question = new Question();
            question.setId(resultSet.getLong("id"));
            question.setStageId(resultSet.getLong("stage_id"));
            question.setType(resultSet.getInt("type"));
            question.setContent(resultSet.getString("content"));
            question.setAnswer(resultSet.getString("answer"));
            return question;
        }, subjectIds.toArray());
        if (questions.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, List<QuestionOption>> optionsByQuestion = questionOptionMapper.selectList(
                        Wrappers.lambdaQuery(QuestionOption.class)
                                .in(QuestionOption::getQuestionId, questionIds)
                                .orderByAsc(QuestionOption::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        Set<String> keys = new HashSet<>();
        for (Question question : questions) {
            List<QuestionImportPreviewVO.OptionPreview> options = optionsByQuestion
                    .getOrDefault(question.getId(), List.of())
                    .stream()
                    .map(option -> new QuestionImportPreviewVO.OptionPreview(
                            option.getOptionKey(),
                            option.getOptionText(),
                            Objects.equals(option.getIsCorrect(), 1)))
                    .toList();
            keys.add(questionFingerprint(question.getStageId(), question.getContent(),
                    question.getType(), question.getAnswer(), options));
        }
        return keys;
    }

    private Long ensureSubject(String subjectName, Actor actor) {
        Subject subject = new Subject();
        subject.setName(subjectName);
        subject.setIcon(DEFAULT_SUBJECT_ICON);
        subject.setDescription(DEFAULT_SUBJECT_DESCRIPTION);
        subject.setThemeColor(DEFAULT_SUBJECT_THEME_COLOR);
        subject.setSortOrder(nextSubjectSortOrder());
        try {
            subjectMapper.insert(subject);
        } catch (DuplicateKeyException ex) {
            // 唯一键对逻辑删除行同样生效：命中已删除学科时恢复。
            Long existingId = jdbcTemplate.query(
                    "SELECT id FROM subject WHERE name = ? LIMIT 1",
                    resultSet -> resultSet.next() ? resultSet.getLong(1) : null,
                    subjectName);
            if (existingId == null) {
                throw new BizException(ErrorCode.CONFLICT,
                        "学科「" + subjectName + "」自动创建失败");
            }
            jdbcTemplate.update("UPDATE subject SET delete_flag = 0 WHERE id = ?", existingId);
            subject.setId(existingId);
        }
        operationLogService.record(actor.userId(), actor.username(), "新增", TARGET_SUBJECT,
                subjectName + "（批量导入自动创建）", actor.ip());
        return subject.getId();
    }

    private int nextSubjectSortOrder() {
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM subject WHERE delete_flag = 0",
                Long.class);
        return Math.toIntExact((max == null ? 0L : max) + 1);
    }

    private Long ensureStage(Long subjectId, String stageName, Actor actor) {
        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(stageName);
        stage.setIcon(null);
        stage.setSortOrder(nextStageSortOrder(subjectId));
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
        } catch (DuplicateKeyException ex) {
            // 唯一键对逻辑删除行同样生效：命中已删除阶段时恢复启用。
            Long existingId = jdbcTemplate.query("""
                            SELECT id FROM stage
                            WHERE subject_id = ? AND name = ? LIMIT 1
                            """,
                    resultSet -> resultSet.next() ? resultSet.getLong(1) : null,
                    subjectId, stageName);
            if (existingId == null) {
                throw new BizException(ErrorCode.CONFLICT,
                        "阶段「" + stageName + "」自动创建失败");
            }
            jdbcTemplate.update("UPDATE stage SET delete_flag = 0, status = 1 WHERE id = ?",
                    existingId);
            stage.setId(existingId);
        }
        operationLogService.record(actor.userId(), actor.username(), "新增", TARGET_STAGE,
                stageName + "（批量导入自动创建）", actor.ip());
        return stage.getId();
    }

    private int nextStageSortOrder(Long subjectId) {
        Long max = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sort_order), 0) FROM stage
                WHERE delete_flag = 0 AND subject_id = ?
                """, Long.class, subjectId);
        return Math.toIntExact((max == null ? 0L : max) + 1);
    }

    // ==================== 内部工具 ====================

    private LambdaQueryWrapper<Question> buildQuery(String keyword, Long subjectId, Long stageId,
                                                    Integer type, Integer difficulty,
                                                    Integer status) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getContent, keyword.trim());
        }
        if (stageId != null) {
            wrapper.eq(Question::getStageId, stageId);
        } else if (subjectId != null) {
            wrapper.apply("stage_id IN (SELECT id FROM stage "
                    + "WHERE delete_flag = 0 AND subject_id = {0})", subjectId);
        }
        if (type != null) {
            wrapper.eq(Question::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (status != null) {
            wrapper.eq(Question::getStatus, status);
        }
        return wrapper;
    }

    private Question requireQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private Stage requireStage(Long stageId) {
        Stage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    private List<QuestionOption> optionsOf(Long questionId) {
        return questionOptionMapper.selectList(Wrappers.lambdaQuery(QuestionOption.class)
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSortOrder)
                .orderByAsc(QuestionOption::getOptionKey));
    }

    private Map<Long, List<QuestionOption>> loadOptions(List<Question> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = questions.stream().map(Question::getId).toList();
        return questionOptionMapper.selectList(Wrappers.lambdaQuery(QuestionOption.class)
                        .in(QuestionOption::getQuestionId, ids)
                        .orderByAsc(QuestionOption::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private List<OptionValue> normalizeOptions(QuestionSaveRequest request) {
        if (!isChoice(request.type())) {
            return List.of();
        }
        if (request.options() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "选择题至少需要 2 个选项");
        }
        List<QuestionSaveRequest.OptionItem> items = request.options().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.optionText() != null && !item.optionText().isBlank())
                .toList();
        if (items.size() < 2) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "选择题至少需要 2 个选项");
        }
        if (items.size() > MAX_OPTION_COUNT) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    "选项不能超过 " + MAX_OPTION_COUNT + " 个");
        }

        List<OptionValue> values = new ArrayList<>();
        int correctCount = 0;
        for (int i = 0; i < items.size(); i++) {
            String text = items.get(i).optionText().trim();
            if (text.length() > MAX_OPTION_TEXT_LENGTH) {
                throw new BizException(ErrorCode.VALIDATION_FAILED,
                        "选项内容不能超过 " + MAX_OPTION_TEXT_LENGTH + " 字");
            }
            boolean correct = Boolean.TRUE.equals(items.get(i).correct());
            if (correct) {
                correctCount++;
            }
            values.add(new OptionValue(OPTION_KEYS.get(i), text, correct));
        }
        if (request.type() == TYPE_SINGLE && correctCount != 1) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "单选题需设置 1 个正确答案");
        }
        if (request.type() == TYPE_MULTIPLE && correctCount < 1) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "多选题至少设置 1 个正确答案");
        }
        return values;
    }

    private String normalizeAnswer(QuestionSaveRequest request) {
        if (isChoice(request.type())) {
            return null;
        }
        String raw = normalizeOptional(request.answer());
        if (request.type() == TYPE_JUDGMENT) {
            String value = normalizeJudgment(raw);
            if (value == null) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "判断题答案需为正确或错误");
            }
            return value;
        }
        return raw;
    }

    private void insertOptions(Long questionId, List<OptionValue> options) {
        for (int i = 0; i < options.size(); i++) {
            OptionValue value = options.get(i);
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(value.key());
            option.setOptionText(value.text());
            option.setIsCorrect(value.correct() ? 1 : 0);
            option.setSortOrder(i);
            questionOptionMapper.insert(option);
        }
    }

    private void replaceOptions(Long questionId, List<OptionValue> options) {
        jdbcTemplate.update("DELETE FROM question_option WHERE question_id = ?", questionId);
        insertOptions(questionId, options);
    }

    private void applyAudit(Question question, Boolean pass, String rejectReason) {
        boolean approved = Boolean.TRUE.equals(pass);
        String reason = approved ? null : normalizeOptional(rejectReason);
        if (!approved && reason == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "驳回时必须填写驳回原因");
        }
        questionMapper.update(null, Wrappers.lambdaUpdate(Question.class)
                .eq(Question::getId, question.getId())
                .set(Question::getStatus, approved ? STATUS_ON : STATUS_REJECTED)
                .set(Question::getRejectReason, reason));
    }

    private boolean canTransition(Question question, String action) {
        if ("off".equals(action)) {
            return Objects.equals(question.getStatus(), STATUS_ON);
        }
        return Objects.equals(question.getStatus(), STATUS_OFF);
    }

    private int transitionStatus(Question question, String action) {
        if (!canTransition(question, action)) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "当前状态不支持该操作");
        }
        return "off".equals(action) ? STATUS_OFF : STATUS_PENDING;
    }

    private long countReferences(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return 0;
        }
        String placeholders = questionIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Long practice = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM practice_record WHERE delete_flag = 0 AND question_id IN ("
                        + placeholders + ")", Long.class, questionIds.toArray());
        Long wrong = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wrong_question WHERE delete_flag = 0 AND question_id IN ("
                        + placeholders + ")", Long.class, questionIds.toArray());
        return (practice == null ? 0L : practice) + (wrong == null ? 0L : wrong);
    }

    private String previewKey(Long userId, String token) {
        return PREVIEW_KEY_PREFIX + userId + ":" + token;
    }

    private Integer parseType(String raw) {
        String value = raw == null ? "" : raw.trim();
        return switch (value) {
            case "单选", "单选题", "1" -> TYPE_SINGLE;
            case "多选", "多选题", "2" -> TYPE_MULTIPLE;
            case "判断", "判断题", "3" -> TYPE_JUDGMENT;
            case "简答", "简答题", "4" -> TYPE_SHORT_ANSWER;
            default -> null;
        };
    }

    private Integer parseDifficulty(String raw) {
        String value = raw == null ? "" : raw.trim();
        return switch (value) {
            case "简单", "容易", "1" -> DIFFICULTY_EASY;
            case "中等", "一般", "2" -> DIFFICULTY_MEDIUM;
            case "困难", "难", "3" -> DIFFICULTY_HARD;
            default -> null;
        };
    }

    private String normalizeJudgment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.equals("正确") || value.equals("对") || value.equals("√")
                || value.equalsIgnoreCase("A") || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("T") || value.equals("1")) {
            return "正确";
        }
        if (value.equals("错误") || value.equals("错") || value.equals("×")
                || value.equalsIgnoreCase("B") || value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("F") || value.equals("0")) {
            return "错误";
        }
        return null;
    }

    private List<String> parseAnswerKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String part : ANSWER_SEPARATOR.split(raw.trim())) {
            String key = part.trim().toUpperCase();
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return new ArrayList<>(keys);
    }

    private String fingerprint(String subjectName, String stageName, String content, Integer type,
                               String answer, List<QuestionImportPreviewVO.OptionPreview> options) {
        StringBuilder builder = new StringBuilder();
        builder.append(subjectName).append('\u0000')
                .append(stageName).append('\u0000')
                .append(content).append('\u0000')
                .append(type == null ? "" : type).append('\u0000')
                .append(answer == null ? "" : answer);
        for (QuestionImportPreviewVO.OptionPreview option : options) {
            builder.append('\u0000')
                    .append(option.optionKey()).append('=')
                    .append(option.optionText());
        }
        return builder.toString();
    }

    /**
     * 库中题目与文件行的统一指纹：阶段 + 题干 + 题型 + 答案 + 选项。
     * 选择题答案由选项正确标识表达，question.answer 为空，故不纳入答案比较。
     */
    private String questionFingerprint(Long stageId, String content, Integer type, String answer,
                                       List<QuestionImportPreviewVO.OptionPreview> options) {
        boolean choice = isChoice(type);
        StringBuilder builder = new StringBuilder();
        builder.append(stageId).append('\u0000')
                .append(content == null ? "" : content.trim()).append('\u0000')
                .append(type == null ? "" : type).append('\u0000')
                .append(choice || answer == null ? "" : answer.trim());
        for (QuestionImportPreviewVO.OptionPreview option : options) {
            builder.append('\u0000')
                    .append(option.optionKey()).append('=')
                    .append(option.optionText() == null ? "" : option.optionText().trim());
        }
        return builder.toString();
    }

    private void putOption(Map<String, String> options, String key, String value) {
        String text = trimToNull(value);
        if (text != null) {
            options.put(key, text);
        }
    }

    private boolean isChoice(Integer type) {
        return Objects.equals(type, TYPE_SINGLE) || Objects.equals(type, TYPE_MULTIPLE);
    }

    private String typeLabel(Integer type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case TYPE_SINGLE -> "单选";
            case TYPE_MULTIPLE -> "多选";
            case TYPE_JUDGMENT -> "判断";
            case TYPE_SHORT_ANSWER -> "简答";
            default -> "";
        };
    }

    private String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return "";
        }
        return switch (difficulty) {
            case DIFFICULTY_EASY -> "简单";
            case DIFFICULTY_MEDIUM -> "中等";
            case DIFFICULTY_HARD -> "困难";
            default -> "";
        };
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_ON -> "已上架";
            case STATUS_OFF -> "已下架";
            case STATUS_PENDING -> "待审核";
            case STATUS_REJECTED -> "已驳回";
            default -> "";
        };
    }

    private <T> byte[] writeExcel(List<T> rows, Class<T> clazz, String sheetName) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, clazz).sheet(sheetName).doWrite(rows);
        return output.toByteArray();
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

    private String firstNonBlank(String first, String second) {
        String value = trim(first);
        return value.isEmpty() ? trim(second) : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptional(String value) {
        return trimToNull(value);
    }

    private String normalizeTags(String tags) {
        String value = trimToNull(tags);
        if (value != null && value.length() > MAX_TAGS_LENGTH) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    "标签不能超过 " + MAX_TAGS_LENGTH + " 字");
        }
        return value;
    }

    private String abbreviate(String content) {
        if (content == null) {
            return "";
        }
        String value = content.trim().replaceAll("\\s+", " ");
        return value.length() <= 40 ? value : value.substring(0, 40) + "…";
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action,
                TARGET_QUESTION, targetName, actor.ip());
    }

    private record OptionValue(String key, String text, boolean correct) {
    }
}
