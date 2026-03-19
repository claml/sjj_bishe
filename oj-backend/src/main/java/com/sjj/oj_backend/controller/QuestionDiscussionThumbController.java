package com.sjj.oj_backend.controller;

import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.model.dto.questiondiscussionthumb.QuestionDiscussionThumbAddRequest;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.service.QuestionDiscussionThumbService;
import com.sjj.oj_backend.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question_discussion_thumb")
public class QuestionDiscussionThumbController {

    @Resource
    private QuestionDiscussionThumbService questionDiscussionThumbService;

    @Resource
    private UserService userService;

    @PostMapping("/")
    public BaseResponse<Integer> doThumb(@RequestBody QuestionDiscussionThumbAddRequest thumbAddRequest,
            HttpServletRequest request) {
        if (thumbAddRequest == null || thumbAddRequest.getDiscussionId() == null
                || thumbAddRequest.getDiscussionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int result = questionDiscussionThumbService.doQuestionDiscussionThumb(
                thumbAddRequest.getDiscussionId(),
                loginUser);
        return ResultUtils.success(result);
    }
}
