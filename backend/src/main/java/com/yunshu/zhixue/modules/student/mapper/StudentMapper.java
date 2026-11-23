package com.yunshu.zhixue.modules.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.modules.student.dto.StudentCountStat;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewRow;
import com.yunshu.zhixue.modules.student.dto.StudentPracticeStat;
import com.yunshu.zhixue.modules.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 学员查询与学习数据聚合（批量 IN + GROUP BY，避免逐行统计）。
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 批量统计刷题数据：学习天数按 Asia/Shanghai 自然日去重（UTC 存储 + 8 小时）。
     */
    @Select("""
            <script>
            SELECT student_id AS studentId,
                   COUNT(DISTINCT DATE(DATE_ADD(practiced_at, INTERVAL 8 HOUR))) AS studyDays,
                   COUNT(*) AS practiceCount,
                   COALESCE(SUM(is_correct), 0) AS correctCount
            FROM practice_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentPracticeStat> selectPracticeStats(@Param("studentIds") Collection<Long> studentIds);

    /**
     * 批量统计 AI 面试次数。
     */
    @Select("""
            <script>
            SELECT student_id AS studentId, COUNT(*) AS total
            FROM interview_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentCountStat> selectInterviewCounts(@Param("studentIds") Collection<Long> studentIds);

    /**
     * 批量统计已学课程数（路线内手动勾选完成）。
     */
    @Select("""
            <script>
            SELECT student_id AS studentId, COUNT(*) AS total
            FROM route_course_completion
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentCountStat> selectCompletedCourseCounts(@Param("studentIds") Collection<Long> studentIds);

    /**
     * 学员面试记录分页，方向名来自 subject（已删除学科显示为空）。
     */
    @Select("""
            SELECT ir.id,
                   ir.subject_id AS subjectId,
                   s.name AS subjectName,
                   ir.difficulty,
                   ir.question_count AS questionCount,
                   ir.overall_score AS overallScore,
                   ir.total_duration AS totalDuration,
                   ir.completed_at AS completedAt
            FROM interview_record ir
            LEFT JOIN subject s ON s.id = ir.subject_id AND s.delete_flag = 0
            WHERE ir.student_id = #{studentId}
              AND ir.delete_flag = 0
            ORDER BY ir.created_at DESC, ir.id DESC
            """)
    IPage<StudentInterviewRow> selectInterviewPage(Page<StudentInterviewRow> page,
                                                   @Param("studentId") Long studentId);
}