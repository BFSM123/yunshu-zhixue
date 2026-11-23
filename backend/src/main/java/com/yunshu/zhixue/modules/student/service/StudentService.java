package com.yunshu.zhixue.modules.student.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.student.dto.StudentDetailVO;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewVO;
import com.yunshu.zhixue.modules.student.dto.StudentVO;
import com.yunshu.zhixue.modules.system.dto.Actor;

import java.util.List;

/**
 * 管理端学员管理（A-U01 ~ A-U08）。
 */
public interface StudentService {

    PageResult<StudentVO> page(String keyword, Integer status, long page, long pageSize);

    StudentDetailVO detail(Long id);

    PageResult<StudentInterviewVO> interviews(Long id, long page, long pageSize);

    void updateStatus(Long id, int status, Actor actor);

    void batchStatus(List<Long> ids, int status, Actor actor);

    void delete(Long id, Actor actor);

    void batchDelete(List<Long> ids, Actor actor);

    byte[] export(String keyword, Integer status, Actor actor);
}