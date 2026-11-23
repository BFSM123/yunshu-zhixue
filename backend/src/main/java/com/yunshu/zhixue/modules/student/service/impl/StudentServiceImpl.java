package com.yunshu.zhixue.modules.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.enums.OperationAction;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.student.dto.StudentDetailVO;
import com.yunshu.zhixue.modules.student.dto.StudentExportRow;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewRow;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewVO;
import com.yunshu.zhixue.modules.student.dto.StudentPracticeStat;
import com.yunshu.zhixue.modules.student.dto.StudentStatsVO;
import com.yunshu.zhixue.modules.student.dto.StudentVO;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.student.service.StudentService;
import com.yunshu.zhixue.modules.system.dto.Actor;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 学员管理。统计口径：明细流水事实聚合（03 §6），学习天数为累计去重上海自然日。
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String TARGET_TYPE = "学员";
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** IN 查询分批大小，避免导出时单条 SQL 参数过多。 */
    private static final int STAT_BATCH_SIZE = 500;

    private final StudentMapper studentMapper;
    private final OperationLogService operationLogService;

    @Override
    public PageResult<StudentVO> page(String keyword, Integer status, long page, long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);

        Page<Student> result = studentMapper.selectPage(new Page<>(pageNo, size),
                buildQuery(keyword, status));
        List<Student> students = result.getRecords();
        List<Long> ids = students.stream().map(Student::getId).toList();
        StatsBundle stats = loadStats(ids);
        List<StudentVO> list = students.stream().map(student -> toVO(student, stats)).toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StudentDetailVO detail(Long id) {
        Student student = requireStudent(id);
        StatsBundle stats = loadStats(List.of(id));
        StudentPracticeStat practice = stats.practice().get(id);
        long practiceCount = valueOf(practice == null ? null : practice.getPracticeCount());
        long correctCount = valueOf(practice == null ? null : practice.getCorrectCount());
        long studyDays = valueOf(practice == null ? null : practice.getStudyDays());

        StudentStatsVO statsVO = new StudentStatsVO(
                studyDays,
                stats.completedCourses().getOrDefault(id, 0L),
                practiceCount,
                accuracyRate(practiceCount, correctCount)
        );
        return new StudentDetailVO(
                student.getId(),
                student.getNickname(),
                student.getAvatar(),
                student.getMobile(),
                student.getEmail(),
                student.getStatus(),
                TimeUtils.toShanghai(student.getCreatedAt()),
                TimeUtils.toShanghai(student.getLastLoginAt()),
                statsVO,
                stats.interviews().getOrDefault(id, 0L)
        );
    }

    @Override
    public PageResult<StudentInterviewVO> interviews(Long id, long page, long pageSize) {
        requireStudent(id);
        long pageNo = Math.max(1, page);
        long size = Math.min(Math.max(1, pageSize), AppConstants.MAX_PAGE_SIZE);
        IPage<StudentInterviewRow> result =
                studentMapper.selectInterviewPage(new Page<>(pageNo, size), id);
        List<StudentInterviewVO> list = result.getRecords().stream()
                .map(row -> new StudentInterviewVO(
                        row.getId(),
                        row.getSubjectId(),
                        row.getSubjectName(),
                        row.getDifficulty(),
                        row.getQuestionCount(),
                        row.getOverallScore(),
                        row.getTotalDuration(),
                        TimeUtils.toShanghai(row.getCompletedAt())
                ))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void updateStatus(Long id, int status, Actor actor) {
        int normalized = statusOrDefault(status);
        Student student = requireStudent(id);
        studentMapper.update(null, Wrappers.lambdaUpdate(Student.class)
                .eq(Student::getId, id)
                .set(Student::getStatus, normalized));
        // 学员端登录尚未实现；禁用由学员端鉴权按 student.status 返回 20005，
        // 待学员端令牌体系落地后在此按 token 域使该学员令牌失效。
        audit(actor, OperationAction.TOGGLE_STATUS.getLabel(), student.getNickname());
    }

    @Override
    @Transactional
    public void batchStatus(List<Long> ids, int status, Actor actor) {
        int normalized = statusOrDefault(status);
        List<Student> students = requireStudents(ids);
        List<Long> studentIds = students.stream().map(Student::getId).toList();
        studentMapper.update(null, Wrappers.lambdaUpdate(Student.class)
                .in(Student::getId, studentIds)
                .set(Student::getStatus, normalized));
        students.forEach(student ->
                audit(actor, OperationAction.TOGGLE_STATUS.getLabel(), student.getNickname()));
    }

    @Override
    public void delete(Long id, Actor actor) {
        Student student = requireStudent(id);
        studentMapper.deleteById(id);
        audit(actor, OperationAction.DELETE.getLabel(), student.getNickname());
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids, Actor actor) {
        List<Student> students = requireStudents(ids);
        students.forEach(student -> {
            studentMapper.deleteById(student.getId());
            audit(actor, OperationAction.DELETE.getLabel(), student.getNickname());
        });
    }

    @Override
    public byte[] export(String keyword, Integer status, Actor actor) {
        LambdaQueryWrapper<Student> query = buildQuery(keyword, status);
        query.last("LIMIT " + AppConstants.MAX_EXPORT_ROWS);
        List<Student> students = studentMapper.selectList(query);
        StatsBundle stats = loadStats(students.stream().map(Student::getId).toList());

        List<StudentExportRow> rows = new java.util.ArrayList<>(students.size());
        int seq = 1;
        for (Student student : students) {
            StudentPracticeStat practice = stats.practice().get(student.getId());
            long practiceCount = valueOf(practice == null ? null : practice.getPracticeCount());
            long correctCount = valueOf(practice == null ? null : practice.getCorrectCount());
            StudentExportRow row = new StudentExportRow();
            row.setSeq(seq++);
            row.setNickname(student.getNickname());
            row.setMobile(student.getMobile());
            row.setEmail(student.getEmail() == null ? "" : student.getEmail());
            row.setCreatedAt(formatTime(student.getCreatedAt()));
            row.setStudyDays(valueOf(practice == null ? null : practice.getStudyDays()));
            row.setPracticeCount(practiceCount);
            row.setAccuracyRate(String.format("%.1f%%",
                    accuracyRate(practiceCount, correctCount)));
            row.setCompletedCourseCount(stats.completedCourses().getOrDefault(student.getId(), 0L));
            row.setInterviewCount(stats.interviews().getOrDefault(student.getId(), 0L));
            row.setStatus(student.getStatus() != null && student.getStatus() == 1 ? "已启用" : "已禁用");
            row.setLastLoginAt(formatTime(student.getLastLoginAt()));
            rows.add(row);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, StudentExportRow.class).sheet("学员列表").doWrite(rows);
        audit(actor, OperationAction.EXPORT.getLabel(), "学员列表");
        return output.toByteArray();
    }

    private LambdaQueryWrapper<Student> buildQuery(String keyword, Integer status) {
        LambdaQueryWrapper<Student> wrapper = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(Student::getNickname, value)
                    .or()
                    .like(Student::getMobile, value));
        }
        if (status != null) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByDesc(Student::getCreatedAt).orderByAsc(Student::getId);
        return wrapper;
    }

    private StatsBundle loadStats(List<Long> studentIds) {
        List<Long> ids = studentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return StatsBundle.empty();
        }
        Map<Long, StudentPracticeStat> practice = new HashMap<>();
        Map<Long, Long> interviews = new HashMap<>();
        Map<Long, Long> completedCourses = new HashMap<>();
        for (int from = 0; from < ids.size(); from += STAT_BATCH_SIZE) {
            List<Long> batch = ids.subList(from, Math.min(from + STAT_BATCH_SIZE, ids.size()));
            studentMapper.selectPracticeStats(batch)
                    .forEach(stat -> practice.put(stat.getStudentId(), stat));
            studentMapper.selectInterviewCounts(batch)
                    .forEach(stat -> interviews.put(stat.getStudentId(), stat.getTotal()));
            studentMapper.selectCompletedCourseCounts(batch)
                    .forEach(stat -> completedCourses.put(stat.getStudentId(), stat.getTotal()));
        }
        return new StatsBundle(practice, interviews, completedCourses);
    }

    private Student requireStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private List<Student> requireStudents(List<Long> ids) {
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<Student> students = studentMapper.selectBatchIds(distinctIds);
        if (students.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return students;
    }

    private int statusOrDefault(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "状态不合法");
        }
        return status;
    }

    private long valueOf(Long value) {
        return value == null ? 0L : value;
    }

    /** 正确率百分比，保留 1 位小数；无作答记录为 0。 */
    private double accuracyRate(long practiceCount, long correctCount) {
        if (practiceCount <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(correctCount * 100.0 / practiceCount)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatTime(LocalDateTime utc) {
        LocalDateTime shanghai = TimeUtils.toShanghai(utc);
        return shanghai == null ? "" : shanghai.format(DISPLAY_TIME);
    }

    private StudentVO toVO(Student student, StatsBundle stats) {
        StudentPracticeStat practice = stats.practice().get(student.getId());
        return new StudentVO(
                student.getId(),
                student.getNickname(),
                student.getAvatar(),
                student.getMobile(),
                student.getStatus(),
                TimeUtils.toShanghai(student.getCreatedAt()),
                TimeUtils.toShanghai(student.getLastLoginAt()),
                valueOf(practice == null ? null : practice.getStudyDays()),
                valueOf(practice == null ? null : practice.getPracticeCount()),
                stats.interviews().getOrDefault(student.getId(), 0L)
        );
    }

    private void audit(Actor actor, String action, String targetName) {
        operationLogService.record(actor.userId(), actor.username(), action, TARGET_TYPE,
                targetName, actor.ip());
    }

    /**
     * 学员统计聚合结果。
     */
    private record StatsBundle(Map<Long, StudentPracticeStat> practice,
                               Map<Long, Long> interviews,
                               Map<Long, Long> completedCourses) {

        static StatsBundle empty() {
            return new StatsBundle(Map.of(), Map.of(), Map.of());
        }
    }
}