package com.yunshu.zhixue.modules.subject.service;

import com.yunshu.zhixue.modules.subject.dto.CreateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.CreateSubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCardVO;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsVO;
import com.yunshu.zhixue.modules.subject.dto.UpdateStageRequest;
import com.yunshu.zhixue.modules.subject.dto.UpdateSubjectRequest;
import com.yunshu.zhixue.modules.system.dto.Actor;

import java.util.List;

/**
 * 学科与阶段管理（A-S01 ~ A-S11）。
 */
public interface SubjectService {

    SubjectStatsVO stats();

    List<SubjectCardVO> list();

    void create(CreateSubjectRequest request, Actor actor);

    void update(Long id, UpdateSubjectRequest request, Actor actor);

    void sort(SortRequest request, Actor actor);

    void delete(Long id, Actor actor);

    void createStage(Long subjectId, CreateStageRequest request, Actor actor);

    void updateStage(Long subjectId, Long stageId, UpdateStageRequest request, Actor actor);

    void sortStage(Long subjectId, SortRequest request, Actor actor);

    void updateStageStatus(Long subjectId, Long stageId, Integer status, Actor actor);

    void deleteStage(Long subjectId, Long stageId, Actor actor);
}
