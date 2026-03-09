package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.dto.postcomment.PostCommentQueryRequest;
import com.sjj.oj_backend.model.entity.PostComment;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.PostCommentVO;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface PostCommentService extends IService<PostComment> {

    void validPostComment(PostComment postComment, boolean add);

    Long addPostComment(Long postId, String content, User loginUser);

    Page<PostCommentVO> listPostCommentVOByPage(PostCommentQueryRequest postCommentQueryRequest, HttpServletRequest request);

    List<PostCommentVO> getPostCommentVOList(List<PostComment> postCommentList, HttpServletRequest request);
}
