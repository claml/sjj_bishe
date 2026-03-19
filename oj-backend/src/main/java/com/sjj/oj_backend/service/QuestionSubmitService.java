package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.AcceptedQuestionSubmitVO;
import com.sjj.oj_backend.model.vo.QuestionSubmitVO;

/**
* @author 石家炅
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2026-01-06 11:32:37
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {

    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest 题目提交信息
     * @param loginUser
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser);

    /**
     * List latest accepted submission for each accepted question of a user.
     *
     * @param userId user id
     * @param current page current
     * @param pageSize page size
     * @return accepted submission page
     */
    Page<AcceptedQuestionSubmitVO> listAcceptedQuestionByUserId(long userId, long current, long pageSize);
}
