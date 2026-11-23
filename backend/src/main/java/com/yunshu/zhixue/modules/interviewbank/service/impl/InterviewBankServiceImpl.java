package com.yunshu.zhixue.modules.interviewbank.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.BatchInterviewStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewBatchIdsRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportExcelRow;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPreviewVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportResultVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportTokenPayload;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetailVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionListItemVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTemplateRow;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicVO;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewBankService;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 面试题库管理实现（A-I01 ~ A-I13）。
 */
@Service
@RequiredArgsConstructor
public class InterviewBankServiceImpl implements InterviewBankService {

    private static final String TARGET_INTERVIEW = "面试题";
    private static final String TARGET_TOPIC = "面试主题";
    private static final String TARGET_SUBJECT = "学科";

    private static final String DEFAULT_SUBJECT_ICON = "📚";
    private static final String DEFAULT_SUBJECT_DESCRIPTION = "批量导入面试题时自动创建";
    private static final String DEFAULT_SUBJECT_THEME_COLOR = "#2563EB";

    private static final int DIFFICULTY_EASY = 1;
    private static final int DIFFICULTY_MEDIUM = 2;
    private static final int DIFFICULTY_HARD = 3;

    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 2;

    private static final int SOURCE_MANUAL = 2;
    private static final int SOURCE_IMPORT = 3;

    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_ANSWER_LENGTH = 10_000;
    private static final int MAX_TOPIC_LENGTH = 50;
    private static final int MAX_TAGS_LENGTH = 255;
    private static final int MAX_SUBJECT_NAME_LENGTH = 50;
    private static final long MAX_IMPORT_FILE_SIZE = 10L * 1024 * 1024;
    private static final int DEFAULT_SEARCH_LIMIT = 20;

    private static final String PREVIEW_KEY_PREFIX = "interviewbank:import:preview:";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private static final List<String> REQUIRED_HEADERS = List.of("所属学科", "题干");

    private final InterviewQuestionMapper questionMapper;
    private final InterviewTopicMapper topicMapper;
    private final SubjectMapper subjectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 查询 ====================

    @Override
    public PageResult<InterviewQuestionListItemVO> page(String keyword, Long subjectId, String topic,
                                                        Integer difficulty, Boolean unclassified,
                                                        long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<InterviewQuestion> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(InterviewQuestion::getContent, keyword.trim());
        }
        if (subjectId != null) {
            wrapper.eq(InterviewQuestion::getSubjectId, subjectId);
        }
        if (Boolean.TRUE.equals(unclassified)) {
            wrapper.and(w -> w.isNull(InterviewQuestion::getTopic)
                    .or().eq(InterviewQuestion::getTopic, ""));
        } else if (topic != null && !topic.isBlank()) {
            wrapper.eq(InterviewQuestion::getTopic, topic.trim());
        }
        if (difficulty != null) {
            wrapper.eq(InterviewQuestion::getDifficulty, difficulty);
        }
        wrapper.orderByDesc(InterviewQuestion::getId);

        IPage<InterviewQuestion> result = questionMapper.selectPage(new Page<>(pageNo, size), wrapper);
        return PageResult.of(toListVOs(result.getRecords()), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public InterviewQuestionDetailVO detail(Long id) {
        InterviewQuestion question = requireQuestion(id);
        Subject subject = subjectMapper.selectById(question.getSubjectId());
        long referencedCount = countReferences(id);
        return new InterviewQuestionDetailVO(
                question.getId(),
                question.getContent(),
                question.getSubjectId(),
                subject == null ? null : subject.getName(),
                question.getReferenceAnswer(),
                question.getTopic(),
                question.getDifficulty(),
                question.getStatus(),
                question.getTags(),
                question.getSourceType(),
                question.getSourceDocName(),
                question.getSourceSeq(),
                referencedCount,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    private List<InterviewQuestionListItemVO> toListVOs(List<InterviewQuestion> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Set<Long> subjectIds = questions.stream()
                .map(InterviewQuestion::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> subjectNames = loadSubjectNames(subjectIds);
        return questions.stream()
                .map(question -> new InterviewQuestionListItemVO(
                        question.getId(),
                        question.getContent(),
                        question.getSubjectId(),
                        subjectNames.get(question.getSubjectId()),
                        question.getTopic(),
                        question.getDifficulty(),
                        question.getStatus(),
                        question.getTags(),
                        question.getSourceType(),
                        question.getSourceDocName(),
                        question.getSourceSeq()))
                .toList();
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional
    public void create(InterviewQuestionSaveRequest request, Actor actor) {
        requireSubject(request.subjectId());

        InterviewQuestion question = new InterviewQuestion();
        question.setSubjectId(request.subjectId());
        question.setContent(request.content().trim());
        question.setReferenceAnswer(trimToNull(request.referenceAnswer()));
        question.setTopic(trimToNull(request.topic()));
        question.setDifficulty(request.difficulty());
        question.setTags(normalizeTags(request.tags()));
        question.setSourceType(SOURCE_MANUAL);
        question.setStatus(STATUS_ON);
        questionMapper.insert(question);

        audit(actor, "新增", abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void update(Long id, InterviewQuestionUpdateRequest request, Actor actor) {
        InterviewQuestion existing = requireQuestion(id);
        String topic = trimToNull(request.topic());
        questionMapper.update(null, Wrappers.lambdaUpdate(InterviewQuestion.class)
                .eq(InterviewQuestion::getId, id)
                .set(InterviewQuestion::getTopic, topic)
                .set(InterviewQuestion::getDifficulty, request.difficulty()));
        audit(actor, "修改", abbreviate(existing.getContent()));
    }

    @Override
    @Transactional
    public void delete(Long id, Actor actor) {
        InterviewQuestion question = requireQuestion(id);
        questionMapper.deleteById(id);
        audit(actor, "删除", abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, InterviewQuestionStatusRequest request, Actor actor) {
        InterviewQuestion question = requireQuestion(id);
        int target = transitionStatus(question, request.action());
        questionMapper.update(null, Wrappers.lambdaUpdate(InterviewQuestion.class)
                .eq(InterviewQuestion::getId, id)
                .set(InterviewQuestion::getStatus, target));
        audit(actor, "on".equals(request.action()) ? "上架" : "下架",
                abbreviate(question.getContent()));
    }

    @Override
    @Transactional
    public int batchUpdateStatus(BatchInterviewStatusRequest request, Actor actor) {
        int target = "on".equals(request.action()) ? STATUS_ON : STATUS_OFF;
        List<InterviewQuestion> questions = questionMapper.selectBatchIds(request.ids());
        List<Long> targetIds = questions.stream()
                .filter(question -> canTransition(question, request.action()))
                .map(InterviewQuestion::getId)
                .toList();
        if (targetIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "所选面试题当前状态不支持该操作");
        }
        questionMapper.update(null, Wrappers.lambdaUpdate(InterviewQuestion.class)
                .in(InterviewQuestion::getId, targetIds)
                .set(InterviewQuestion::getStatus, target));
        audit(actor, "on".equals(request.action()) ? "批量上架" : "批量下架",
                "共 " + targetIds.size() + " 道面试题");
        return targetIds.size();
    }

    @Override
    @Transactional
    public int batchDelete(InterviewBatchIdsRequest request, Actor actor) {
        List<InterviewQuestion> questions = questionMapper.selectBatchIds(request.ids());
        if (questions.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "所选面试题不存在");
        }
        List<Long> ids = questions.stream().map(InterviewQuestion::getId).toList();
        questionMapper.deleteBatchIds(ids);
        audit(actor, "批量删除", "共 " + ids.size() + " 道面试题");
        return ids.size();
    }

    // ==================== Excel 导入 ====================

    @Override
    public InterviewImportPreviewVO previewImport(MultipartFile file, Actor actor) {
        validateImportFile(file);

        List<InterviewImportExcelRow> excelRows = readExcelRows(file);
        if (excelRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "表格中没有可导入的数据");
        }

        Map<String, Long> subjectIdsByName = loadSubjectIds();
        Set<String> topicVocabulary = loadTopicVocabulary();
        Set<Long> knownSubjectIds = new HashSet<>(subjectIdsByName.values());
        Set<String> existingKeys = loadExistingKeys(knownSubjectIds);

        List<InterviewImportPreviewVO.Row> rows = new ArrayList<>();
        Map<String, Integer> seenRows = new HashMap<>();
        Set<String> autoCreateSubjects = new LinkedHashSet<>();
        Set<String> newTopics = new LinkedHashSet<>();

        for (InterviewImportExcelRow excelRow : excelRows) {
            InterviewImportPreviewVO.Row row = parseRow(excelRow, subjectIdsByName,
                    topicVocabulary, existingKeys, seenRows);
            rows.add(row);
            if ("invalid".equals(row.status())) {
                continue;
            }
            if (row.subjectAutoCreate() && row.subjectName() != null
                    && !row.subjectName().isBlank()) {
                autoCreateSubjects.add(row.subjectName());
            }
            if (row.topicNew()) {
                newTopics.add(row.topic());
            }
        }

        int validCount = 0;
        int warningCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;
        for (InterviewImportPreviewVO.Row row : rows) {
            switch (row.status()) {
                case "valid" -> validCount++;
                case "warning" -> warningCount++;
                default -> invalidCount++;
            }
            if (row.duplicate() && !"invalid".equals(row.status())) {
                duplicateCount++;
            }
        }

        String sourceDocName = fileNameOf(file.getOriginalFilename());
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = previewKey(actor.userId(), token);
        try {
            InterviewImportTokenPayload payload = new InterviewImportTokenPayload(
                    sourceDocName, rows);
            redisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(payload), PREVIEW_TTL);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
        }

        return new InterviewImportPreviewVO(token, sourceDocName, rows.size(),
                validCount, warningCount, invalidCount, duplicateCount,
                autoCreateSubjects.size(), newTopics.size(), rows);
    }

    @Override
    @Transactional
    public InterviewImportResultVO confirmImport(String token, Actor actor) {
        String key = previewKey(actor.userId(), token.trim());
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "导入预览已失效，请重新解析");
        }

        InterviewImportTokenPayload payload;
        try {
            payload = objectMapper.readValue(json, InterviewImportTokenPayload.class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "导入预览解析失败，请重新解析");
        }

        List<InterviewImportPreviewVO.Row> validRows = payload.rows().stream()
                .filter(row -> !"invalid".equals(row.status()))
                .toList();
        if (validRows.isEmpty()) {
            throw new BizException(ErrorCode.IMPORT_VALIDATION_FAILED, "没有可导入的面试题");
        }

        List<InterviewImportPreviewVO.Row> importable = validRows.stream()
                .filter(row -> !row.duplicate())
                .toList();
        int skippedCount = validRows.size() - importable.size();

        if (importable.isEmpty()) {
            redisTemplate.delete(key);
            return new InterviewImportResultVO(0, skippedCount, 0);
        }

        Map<String, Long> subjectIdsByName = loadSubjectIds();
        int createdSubjectCount = 0;
        long imported = 0;
        for (InterviewImportPreviewVO.Row row : importable) {
            Long subjectId = subjectIdsByName.get(row.subjectName());
            if (subjectId == null) {
                subjectId = ensureSubject(row.subjectName(), actor);
                subjectIdsByName.put(row.subjectName(), subjectId);
                createdSubjectCount++;
            }

            InterviewQuestion question = new InterviewQuestion();
            question.setSubjectId(subjectId);
            question.setContent(row.content());
            question.setReferenceAnswer(row.referenceAnswer());
            question.setTopic(row.topic());
            question.setDifficulty(row.difficulty());
            question.setTags(row.tags());
            question.setSourceType(SOURCE_IMPORT);
            question.setSourceDocName(payload.sourceDocName());
            question.setSourceSeq(Math.max(1, row.row() - 1));
            question.setStatus(STATUS_ON);
            questionMapper.insert(question);
            imported++;
        }

        redisTemplate.delete(key);
        audit(actor, "新增", "批量导入 " + imported + " 道面试题"
                + (skippedCount > 0 ? "（跳过重复 " + skippedCount + " 道）" : ""));
        return new InterviewImportResultVO(imported, skippedCount, createdSubjectCount);
    }

    @Override
    public byte[] template() {
        InterviewTemplateRow first = new InterviewTemplateRow();
        first.setSubject("AI应用与智能体开发");
        first.setContent("Ollama 是什么，有什么作用？");
        first.setDifficulty("中等");
        first.setTopic("大模型与本地部署");
        first.setTags("Ollama,本地部署,性能优化");
        first.setReferenceAnswer("Ollama 是一个轻量级的本地大模型部署工具，"
                + "支持在本地或服务器上部署和管理多个大模型（如 Llama、Qwen），"
                + "核心作用是本地推理与模型管理。");

        InterviewTemplateRow second = new InterviewTemplateRow();
        second.setSubject("AI应用与智能体开发");
        second.setContent("说一说 RAG 的实现原理。");
        second.setDifficulty("中等");
        second.setTopic("RAG检索增强");
        second.setTags("RAG,向量数据库,知识库");
        second.setReferenceAnswer("将文档切片向量化后存入向量数据库；用户提问时先检索相关片段，"
                + "再把片段作为上下文交给大模型生成回答。");

        return writeExcel(List.of(first, second), InterviewTemplateRow.class, "面试题");
    }

    // ==================== 主题词表 ====================

    @Override
    public List<InterviewTopicVO> topics() {
        List<InterviewTopic> vocabulary = topicMapper.selectList(
                Wrappers.lambdaQuery(InterviewTopic.class)
                        .orderByAsc(InterviewTopic::getSortOrder)
                        .orderByAsc(InterviewTopic::getId));
        Map<Long, String> subjectNames = loadSubjectNames(vocabulary.stream()
                .map(InterviewTopic::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<String, Long> countByTopic = new HashMap<>();
        Map<String, Long> countBySubjectTopic = new HashMap<>();
        List<Map<String, Object>> grouped = jdbcTemplate.queryForList("""
                SELECT subject_id, topic, COUNT(*) AS cnt
                FROM interview_question
                WHERE delete_flag = 0 AND topic IS NOT NULL AND topic <> ''
                GROUP BY subject_id, topic
                """);
        for (Map<String, Object> item : grouped) {
            String topic = Objects.toString(item.get("topic"), "");
            long count = longValue(item.get("cnt"));
            Long subjectId = item.get("subject_id") == null
                    ? null : ((Number) item.get("subject_id")).longValue();
            countByTopic.merge(topic, count, Long::sum);
            if (subjectId != null) {
                countBySubjectTopic.merge(subjectTopicKey(subjectId, topic), count, Long::sum);
            }
        }

        Set<String> vocabularyNames = vocabulary.stream()
                .map(InterviewTopic::getName)
                .collect(Collectors.toSet());

        List<InterviewTopicVO> result = new ArrayList<>();
        for (InterviewTopic topic : vocabulary) {
            // 全局主题 subjectId 为空，不能直接对不可变空 Map 取 null 键。
            String subjectName = topic.getSubjectId() == null
                    ? null : subjectNames.get(topic.getSubjectId());
            long count = topic.getSubjectId() == null
                    ? countByTopic.getOrDefault(topic.getName(), 0L)
                    : countBySubjectTopic.getOrDefault(
                            subjectTopicKey(topic.getSubjectId(), topic.getName()), 0L);
            result.add(new InterviewTopicVO(topic.getId(), topic.getName(),
                    topic.getSubjectId(), subjectName,
                    topic.getSortOrder(), count, true));
        }

        List<InterviewTopicVO> outOfVocabulary = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByTopic.entrySet()) {
            if (!vocabularyNames.contains(entry.getKey())) {
                outOfVocabulary.add(new InterviewTopicVO(null, entry.getKey(), null,
                        null, null, entry.getValue(), false));
            }
        }
        outOfVocabulary.sort(Comparator.comparingLong(InterviewTopicVO::questionCount).reversed()
                .thenComparing(InterviewTopicVO::name));
        result.addAll(outOfVocabulary);
        return result;
    }

    @Override
    @Transactional
    public void createTopic(InterviewTopicSaveRequest request, Actor actor) {
        String name = request.name().trim();
        Long subjectId = request.subjectId();
        if (subjectId != null) {
            requireSubject(subjectId);
        }
        checkTopicNameUnique(name, subjectId, null);

        InterviewTopic topic = new InterviewTopic();
        topic.setName(name);
        topic.setSubjectId(subjectId);
        topic.setSortOrder(request.sortOrder() == null
                ? nextTopicSortOrder(subjectId) : request.sortOrder());
        topicMapper.insert(topic);

        auditTopic(actor, "新增", name);
    }

    @Override
    @Transactional
    public void updateTopic(Long id, InterviewTopicSaveRequest request, Actor actor) {
        InterviewTopic existing = requireTopic(id);
        String name = request.name().trim();
        Long subjectId = request.subjectId();
        if (subjectId != null) {
            requireSubject(subjectId);
        }
        checkTopicNameUnique(name, subjectId, id);

        int sortOrder = request.sortOrder() == null
                ? existing.getSortOrder() : request.sortOrder();

        if (!Objects.equals(existing.getName(), name)) {
            // 改名时同步更新题目主题，避免原主题变成词表外主题。
            LambdaUpdateWrapper<InterviewQuestion> wrapper =
                    Wrappers.lambdaUpdate(InterviewQuestion.class)
                            .eq(InterviewQuestion::getTopic, existing.getName())
                            .set(InterviewQuestion::getTopic, name);
            if (existing.getSubjectId() != null) {
                wrapper.eq(InterviewQuestion::getSubjectId, existing.getSubjectId());
            }
            questionMapper.update(null, wrapper);
        }

        topicMapper.update(null, Wrappers.lambdaUpdate(InterviewTopic.class)
                .eq(InterviewTopic::getId, id)
                .set(InterviewTopic::getName, name)
                .set(InterviewTopic::getSubjectId, subjectId)
                .set(InterviewTopic::getSortOrder, sortOrder));

        auditTopic(actor, "修改", name);
    }

    @Override
    @Transactional
    public void deleteTopic(Long id, Actor actor) {
        InterviewTopic topic = requireTopic(id);
        LambdaQueryWrapper<InterviewQuestion> wrapper =
                Wrappers.lambdaQuery(InterviewQuestion.class)
                        .eq(InterviewQuestion::getTopic, topic.getName());
        if (topic.getSubjectId() != null) {
            wrapper.eq(InterviewQuestion::getSubjectId, topic.getSubjectId());
        }
        long used = questionMapper.selectCount(wrapper);
        if (used > 0) {
            throw new BizException(ErrorCode.RELATED_CONTENT_EXISTS,
                    "该主题下还有 " + used + " 道面试题，请先调整题目主题");
        }
        topicMapper.deleteById(id);
        auditTopic(actor, "删除", topic.getName());
    }

    // ==================== 检索测试 ====================

    @Override
    public InterviewSearchTestVO searchTest(InterviewSearchTestRequest request) {
        String keyword = request.keyword().trim();
        int limit = request.limit() == null ? DEFAULT_SEARCH_LIMIT : request.limit();

        LambdaQueryWrapper<InterviewQuestion> wrapper =
                Wrappers.lambdaQuery(InterviewQuestion.class)
                        .like(InterviewQuestion::getContent, keyword);
        if (request.subjectId() != null) {
            wrapper.eq(InterviewQuestion::getSubjectId, request.subjectId());
        }

        long total = questionMapper.selectCount(wrapper);
        wrapper.orderByDesc(InterviewQuestion::getId).last("LIMIT " + limit);
        List<InterviewQuestion> questions = questionMapper.selectList(wrapper);

        Map<Long, String> subjectNames = loadSubjectNames(questions.stream()
                .map(InterviewQuestion::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<InterviewSearchTestVO.Item> items = questions.stream()
                .map(question -> new InterviewSearchTestVO.Item(
                        question.getId(),
                        question.getContent(),
                        question.getSubjectId(),
                        subjectNames.get(question.getSubjectId()),
                        question.getTopic(),
                        question.getDifficulty(),
                        question.getSourceType(),
                        question.getSourceDocName(),
                        question.getSourceSeq()))
                .toList();
        return new InterviewSearchTestVO(keyword, total, items);
    }

    // ==================== 导入内部逻辑 ====================

    private InterviewImportPreviewVO.Row parseRow(InterviewImportExcelRow data,
                                                  Map<String, Long> subjectIdsByName,
                                                  Set<String> topicVocabulary,
                                                  Set<String> existingKeys,
                                                  Map<String, Integer> seenRows) {
        int lineNo = data.getRowNo() == null ? 0 : data.getRowNo();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String content = trim(data.getContent());
        String subjectName = trim(data.getSubject());
        String topic = trimToNull(data.getTopic());
        String tags = trimToNull(data.getTags());
        String referenceAnswer = trimToNull(data.getReferenceAnswer());
        String difficultyRaw = trim(data.getDifficulty());

        if (content.isEmpty()) {
            errors.add("题干不能为空");
        } else if (content.length() > MAX_CONTENT_LENGTH) {
            errors.add("题干不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        if (subjectName.isEmpty()) {
            errors.add("所属学科不能为空");
        } else if (subjectName.length() > MAX_SUBJECT_NAME_LENGTH) {
            errors.add("学科名称不能超过 " + MAX_SUBJECT_NAME_LENGTH + " 字");
        }
        if (topic != null && topic.length() > MAX_TOPIC_LENGTH) {
            errors.add("主题不能超过 " + MAX_TOPIC_LENGTH + " 字");
        }
        if (tags != null && tags.length() > MAX_TAGS_LENGTH) {
            errors.add("标签不能超过 " + MAX_TAGS_LENGTH + " 字");
        }
        if (referenceAnswer != null && referenceAnswer.length() > MAX_ANSWER_LENGTH) {
            errors.add("参考答案不能超过 " + MAX_ANSWER_LENGTH + " 字");
        }

        Integer difficulty = parseDifficulty(difficultyRaw);
        if (difficulty == null && !difficultyRaw.isEmpty()) {
            errors.add("难度需为简单/中等/困难");
        } else if (difficulty == null) {
            difficulty = DIFFICULTY_MEDIUM;
        }

        Long subjectId = subjectName.isEmpty() ? null : subjectIdsByName.get(subjectName);
        boolean subjectAutoCreate = !subjectName.isEmpty() && subjectId == null;
        if (subjectAutoCreate) {
            warnings.add("学科「" + subjectName + "」不存在，导入时将自动创建");
        }

        boolean topicNew = topic != null && !topicVocabulary.contains(topic);

        boolean duplicate = false;
        if (!content.isEmpty() && !subjectName.isEmpty()) {
            String fileKey = subjectName + "\u0000" + content;
            Integer firstRow = seenRows.get(fileKey);
            if (firstRow != null) {
                duplicate = true;
                warnings.add("与第 " + firstRow + " 行题目重复，将跳过导入");
            } else {
                seenRows.put(fileKey, lineNo);
            }
            if (subjectId != null && existingKeys.contains(subjectId + "\u0000" + content)) {
                duplicate = true;
                warnings.add("该学科下已存在相同题干，将跳过导入");
            }
            if (errors.isEmpty() && !duplicate && subjectId != null) {
                existingKeys.add(subjectId + "\u0000" + content);
            }
        }

        List<String> messages = new ArrayList<>(errors);
        messages.addAll(warnings);
        String status = !errors.isEmpty() ? "invalid" : !warnings.isEmpty() ? "warning" : "valid";
        return new InterviewImportPreviewVO.Row(
                lineNo, content, subjectName, subjectId, subjectAutoCreate,
                topic, topicNew, difficulty, tags, referenceAnswer,
                duplicate, status, List.copyOf(messages));
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

    private List<InterviewImportExcelRow> readExcelRows(MultipartFile file) {
        List<InterviewImportExcelRow> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, InterviewImportExcelRow.class,
                    new AnalysisEventListener<InterviewImportExcelRow>() {
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
                        public void invoke(InterviewImportExcelRow data,
                                           AnalysisContext context) {
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

    private Map<Long, String> loadSubjectNames(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName, (left, right) -> left));
    }

    private Set<String> loadTopicVocabulary() {
        return topicMapper.selectList(Wrappers.lambdaQuery(InterviewTopic.class))
                .stream()
                .map(InterviewTopic::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> loadExistingKeys(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return new HashSet<>();
        }
        String placeholders = subjectIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT subject_id, content FROM interview_question "
                        + "WHERE delete_flag = 0 AND subject_id IN (" + placeholders + ")",
                subjectIds.toArray());
        Set<String> keys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Long subjectId = ((Number) row.get("subject_id")).longValue();
            keys.add(subjectId + "\u0000" + trim(Objects.toString(row.get("content"), "")));
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

    private int nextTopicSortOrder(Long subjectId) {
        LambdaQueryWrapper<InterviewTopic> wrapper = Wrappers.lambdaQuery();
        if (subjectId == null) {
            wrapper.isNull(InterviewTopic::getSubjectId);
        } else {
            wrapper.eq(InterviewTopic::getSubjectId, subjectId);
        }
        List<InterviewTopic> topics = topicMapper.selectList(wrapper);
        int max = topics.stream()
                .map(InterviewTopic::getSortOrder)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        return max + 1;
    }

    private void checkTopicNameUnique(String name, Long subjectId, Long excludeId) {
        LambdaQueryWrapper<InterviewTopic> wrapper = Wrappers.lambdaQuery(InterviewTopic.class)
                .eq(InterviewTopic::getName, name);
        if (subjectId == null) {
            wrapper.isNull(InterviewTopic::getSubjectId);
        } else {
            wrapper.eq(InterviewTopic::getSubjectId, subjectId);
        }
        if (excludeId != null) {
            wrapper.ne(InterviewTopic::getId, excludeId);
        }
        if (topicMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.NAME_DUPLICATED, "该主题名称已存在");
        }
    }

    // ==================== 内部工具 ====================

    private InterviewQuestion requireQuestion(Long id) {
        InterviewQuestion question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "面试题不存在");
        }
        return question;
    }

    private InterviewTopic requireTopic(Long id) {
        InterviewTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "主题不存在");
        }
        return topic;
    }

    private void requireSubject(Long subjectId) {
        if (subjectMapper.selectById(subjectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "所属学科不存在");
        }
    }

    private long countReferences(Long questionId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT interview_record_id)
                FROM interview_question_score
                WHERE delete_flag = 0 AND question_id = ?
                """, Long.class, questionId);
        return count == null ? 0L : count;
    }

    private boolean canTransition(InterviewQuestion question, String action) {
        if ("off".equals(action)) {
            return Objects.equals(question.getStatus(), STATUS_ON);
        }
        return Objects.equals(question.getStatus(), STATUS_OFF);
    }

    private int transitionStatus(InterviewQuestion question, String action) {
        if (!canTransition(question, action)) {
            throw new BizException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "当前状态不支持该操作");
        }
        return "off".equals(action) ? STATUS_OFF : STATUS_ON;
    }

    private String previewKey(Long userId, String token) {
        return PREVIEW_KEY_PREFIX + userId + ":" + token;
    }

    private String subjectTopicKey(Long subjectId, String topic) {
        return subjectId + "\u0000" + topic;
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

    private String normalizeTags(String tags) {
        String value = trimToNull(tags);
        if (value != null && value.length() > MAX_TAGS_LENGTH) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    "标签不能超过 " + MAX_TAGS_LENGTH + " 字");
        }
        return value;
    }

    private String fileNameOf(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "未命名文件.xlsx";
        }
        String name = originalFilename.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash >= 0 ? name.substring(slash + 1) : name;
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
                TARGET_INTERVIEW, targetName, actor.ip());
    }

    private void auditTopic(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action,
                TARGET_TOPIC, targetName, actor.ip());
    }
}