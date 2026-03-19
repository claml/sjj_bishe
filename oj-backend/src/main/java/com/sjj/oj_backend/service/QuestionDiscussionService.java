package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.dto.questiondiscussion.QuestionDiscussionQueryRequest;
import com.sjj.oj_backend.model.entity.QuestionDiscussion;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.QuestionDiscussionVO;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface QuestionDiscussionService extends IService<QuestionDiscussion> {

    void validQuestionDiscussion(QuestionDiscussion questionDiscussion, boolean add);

    Long addQuestionDiscussion(Long questionId, String content, User loginUser);

    boolean deleteQuestionDiscussion(Long discussionId, User loginUser, boolean admin);

    Page<QuestionDiscussionVO> listQuestionDiscussionVOByPage(
            QuestionDiscussionQueryRequest questionDiscussionQueryRequest,
            HttpServletRequest request);

    List<QuestionDiscussionVO> getQuestionDiscussionVOList(
            List<QuestionDiscussion> questionDiscussionList,
            HttpServletRequest request);
}
