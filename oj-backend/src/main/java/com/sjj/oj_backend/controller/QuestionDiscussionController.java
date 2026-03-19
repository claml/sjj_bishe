package com.sjj.oj_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.DeleteRequest;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.model.dto.questiondiscussion.QuestionDiscussionAddRequest;
import com.sjj.oj_backend.model.dto.questiondiscussion.QuestionDiscussionQueryRequest;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.QuestionDiscussionVO;
import com.sjj.oj_backend.service.QuestionDiscussionService;
import com.sjj.oj_backend.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question_discussion")
public class QuestionDiscussionController {

    @Resource
    private QuestionDiscussionService questionDiscussionService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addQuestionDiscussion(@RequestBody QuestionDiscussionAddRequest addRequest,
            HttpServletRequest request) {
        if (addRequest == null || addRequest.getQuestionId() == null || addRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(addRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "discussion can not be empty");
        }
        User loginUser = userService.getLoginUser(request);
        Long discussionId = questionDiscussionService.addQuestionDiscussion(
                addRequest.getQuestionId(),
                addRequest.getContent().trim(),
                loginUser);
        return ResultUtils.success(discussionId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionDiscussion(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = questionDiscussionService.deleteQuestionDiscussion(
                deleteRequest.getId(),
                loginUser,
                userService.isAdmin(loginUser));
        return ResultUtils.success(result);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionDiscussionVO>> listQuestionDiscussionVOByPage(
            @RequestBody QuestionDiscussionQueryRequest queryRequest,
            HttpServletRequest request) {
        return ResultUtils.success(questionDiscussionService.listQuestionDiscussionVOByPage(queryRequest, request));
    }
}
