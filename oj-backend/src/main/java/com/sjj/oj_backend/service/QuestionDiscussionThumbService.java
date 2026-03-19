package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.entity.QuestionDiscussionThumb;
import com.sjj.oj_backend.model.entity.User;

public interface QuestionDiscussionThumbService extends IService<QuestionDiscussionThumb> {

    int doQuestionDiscussionThumb(long discussionId, User loginUser);

    int doQuestionDiscussionThumbInner(long userId, long discussionId);
}
