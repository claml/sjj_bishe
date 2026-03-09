package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.mapper.PostCommentMapper;
import com.sjj.oj_backend.model.dto.postcomment.PostCommentQueryRequest;
import com.sjj.oj_backend.model.entity.Post;
import com.sjj.oj_backend.model.entity.PostComment;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.PostCommentVO;
import com.sjj.oj_backend.service.PostCommentService;
import com.sjj.oj_backend.service.PostService;
import com.sjj.oj_backend.service.UserService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentService {

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @Override
    public void validPostComment(PostComment postComment, boolean add) {
        ThrowUtils.throwIf(postComment == null, ErrorCode.PARAMS_ERROR);
        if (add) {
            ThrowUtils.throwIf(postComment.getPostId() == null || postComment.getPostId() <= 0, ErrorCode.PARAMS_ERROR, "帖子不存在");
            ThrowUtils.throwIf(StringUtils.isBlank(postComment.getContent()), ErrorCode.PARAMS_ERROR, "评论不能为空");
        }
        if (StringUtils.isNotBlank(postComment.getContent()) && postComment.getContent().length() > 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论过长");
        }
    }

    @Override
    public Long addPostComment(Long postId, String content, User loginUser) {
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        PostComment postComment = new PostComment();
        postComment.setPostId(postId);
        postComment.setContent(content);
        postComment.setUserId(loginUser.getId());
        validPostComment(postComment, true);
        boolean result = this.save(postComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "评论失败");
        return postComment.getId();
    }

    @Override
    public Page<PostCommentVO> listPostCommentVOByPage(PostCommentQueryRequest postCommentQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(postCommentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = postCommentQueryRequest.getCurrent();
        long pageSize = postCommentQueryRequest.getPageSize();
        Long postId = postCommentQueryRequest.getPostId();
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR);

        QueryWrapper<PostComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("postId", postId);
        queryWrapper.orderByAsc("createTime");
        Page<PostComment> page = this.page(new Page<>(current, pageSize), queryWrapper);

        Page<PostCommentVO> voPage = new Page<>(current, pageSize, page.getTotal());
        voPage.setRecords(getPostCommentVOList(page.getRecords(), request));
        return voPage;
    }

    @Override
    public List<PostCommentVO> getPostCommentVOList(List<PostComment> postCommentList, HttpServletRequest request) {
        if (postCommentList == null || postCommentList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIdSet = postCommentList.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        return postCommentList.stream().map(comment -> {
            PostCommentVO vo = new PostCommentVO();
            BeanUtils.copyProperties(comment, vo);
            User user = userIdUserListMap.getOrDefault(comment.getUserId(), Collections.emptyList()).stream().findFirst().orElse(null);
            vo.setUser(userService.getUserVO(user));
            return vo;
        }).collect(Collectors.toList());
    }
}
