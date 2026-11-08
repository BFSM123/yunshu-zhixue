package com.yunshu.zhixue.modules.course.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewVO;
import com.yunshu.zhixue.modules.course.dto.CourseImportResultVO;
import com.yunshu.zhixue.modules.course.dto.CourseStatsVO;
import com.yunshu.zhixue.modules.course.dto.CourseVO;
import com.yunshu.zhixue.modules.course.dto.CreateCourseRequest;
import com.yunshu.zhixue.modules.course.dto.UpdateCourseRequest;
import com.yunshu.zhixue.modules.system.dto.Actor;
import org.springframework.web.multipart.MultipartFile;

/**
 * B 站课程库（A-C01 ~ A-C07）。
 */
public interface CourseService {

    CourseStatsVO stats();

    PageResult<CourseVO> page(String keyword, Long subjectId, Integer source,
                              long page, long pageSize);

    void create(CreateCourseRequest request, Actor actor);

    void update(Long id, UpdateCourseRequest request, Actor actor);

    void delete(Long id, Actor actor);

    CourseImportPreviewVO previewImport(MultipartFile file, Actor actor);

    CourseImportResultVO confirmImport(String token, Actor actor);
}
