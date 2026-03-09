package com.sjj.oj_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.model.dto.postcomment.PostCommentAddRequest;
import com.sjj.oj_backend.model.dto.postcomment.PostCommentQueryRequest;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.PostCommentVO;
import com.sjj.oj_backend.service.PostCommentService;
import com.sjj.oj_backend.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post_comment")
public class PostCommentController {

    @Resource
    private PostCommentService postCommentService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addPostComment(@RequestBody PostCommentAddRequest postCommentAddRequest, HttpServletRequest request) {
        if (postCommentAddRequest == null || postCommentAddRequest.getPostId() == null || postCommentAddRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(postCommentAddRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论不能为空");
        }
        User loginUser = userService.getLoginUser(request);
        Long commentId = postCommentService.addPostComment(postCommentAddRequest.getPostId(), postCommentAddRequest.getContent().trim(), loginUser);
        return ResultUtils.success(commentId);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PostCommentVO>> listPostCommentVOByPage(@RequestBody PostCommentQueryRequest postCommentQueryRequest,
                                                                      HttpServletRequest request) {
        return ResultUtils.success(postCommentService.listPostCommentVOByPage(postCommentQueryRequest, request));
    }
}
