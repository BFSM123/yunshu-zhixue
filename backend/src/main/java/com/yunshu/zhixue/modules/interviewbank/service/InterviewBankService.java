package com.yunshu.zhixue.modules.interviewbank.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.BatchInterviewStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewBatchIdsRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportPreviewVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewImportResultVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetailVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionListItemVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewSearchTestVO;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicVO;
import com.yunshu.zhixue.modules.system.dto.Actor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 面试题库管理（A-I01 ~ A-I13，含状态流转与 Excel 导入）。
 */
public interface InterviewBankService {

    PageResult<InterviewQuestionListItemVO> page(String keyword, Long subjectId, String topic,
                                                 Integer difficulty, Boolean unclassified,
                                                 long page, long pageSize);

    InterviewQuestionDetailVO detail(Long id);

    void create(InterviewQuestionSaveRequest request, Actor actor);

    void update(Long id, InterviewQuestionUpdateRequest request, Actor actor);

    void delete(Long id, Actor actor);

    void updateStatus(Long id, InterviewQuestionStatusRequest request, Actor actor);

    int batchUpdateStatus(BatchInterviewStatusRequest request, Actor actor);

    int batchDelete(InterviewBatchIdsRequest request, Actor actor);

    InterviewImportPreviewVO previewImport(MultipartFile file, Actor actor);

    InterviewImportResultVO confirmImport(String token, Actor actor);

    byte[] template();

    List<InterviewTopicVO> topics();

    void createTopic(InterviewTopicSaveRequest request, Actor actor);

    void updateTopic(Long id, InterviewTopicSaveRequest request, Actor actor);

    void deleteTopic(Long id, Actor actor);

    InterviewSearchTestVO searchTest(InterviewSearchTestRequest request);
}