package com.sjj.oj_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitAddRequest;
//import com.sjj.oj_backend.model.dto.questionthumb.QuestionThumbAddRequest;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionAcceptedQueryRequest;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.AcceptedQuestionSubmitVO;
import com.sjj.oj_backend.model.vo.QuestionSubmitVO;
import com.sjj.oj_backend.service.QuestionSubmitService;
//import com.sjj.oj_backend.service.QuestionThumbService;
import com.sjj.oj_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.QuestionMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题目提交接口
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能点赞
        final User loginUser = userService.getLoginUser(request);
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        final User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser)) {
            questionSubmitQueryRequest.setUserId(loginUser.getId());
        }
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * List user's accepted questions with latest accepted code.
     *
     * @param acceptedQueryRequest query request
     * @return paged accepted question submissions
     */
    @PostMapping("/accepted/list/page")
    public BaseResponse<Page<AcceptedQuestionSubmitVO>> listAcceptedQuestionSubmitByPage(
            @RequestBody QuestionAcceptedQueryRequest acceptedQueryRequest,
            HttpServletRequest request) {
        if (acceptedQueryRequest == null || acceptedQueryRequest.getUserId() == null
                || acceptedQueryRequest.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = acceptedQueryRequest.getCurrent();
        long size = acceptedQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "pageSize too large");
        long userId = acceptedQueryRequest.getUserId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null && loginUser.getId() != null && loginUser.getId() > 0
                && loginUser.getId() != userId
                && userService.getById(userId) == null) {
            userId = loginUser.getId();
        }

        Page<AcceptedQuestionSubmitVO> page = questionSubmitService.listAcceptedQuestionByUserId(
                userId, current, size);
        return ResultUtils.success(page);
    }

}
