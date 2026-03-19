package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.mapper.QuestionDiscussionThumbMapper;
import com.sjj.oj_backend.model.entity.QuestionDiscussion;
import com.sjj.oj_backend.model.entity.QuestionDiscussionThumb;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.service.QuestionDiscussionService;
import com.sjj.oj_backend.service.QuestionDiscussionThumbService;
import javax.annotation.Resource;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionDiscussionThumbServiceImpl
        extends ServiceImpl<QuestionDiscussionThumbMapper, QuestionDiscussionThumb>
        implements QuestionDiscussionThumbService {

    @Resource
    private QuestionDiscussionService questionDiscussionService;

    @Override
    public int doQuestionDiscussionThumb(long discussionId, User loginUser) {
        QuestionDiscussion discussion = questionDiscussionService.getById(discussionId);
        if (discussion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        long userId = loginUser.getId();
        QuestionDiscussionThumbService proxy = (QuestionDiscussionThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return proxy.doQuestionDiscussionThumbInner(userId, discussionId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doQuestionDiscussionThumbInner(long userId, long discussionId) {
        QuestionDiscussionThumb questionDiscussionThumb = new QuestionDiscussionThumb();
        questionDiscussionThumb.setUserId(userId);
        questionDiscussionThumb.setDiscussionId(discussionId);
        QueryWrapper<QuestionDiscussionThumb> thumbQueryWrapper = new QueryWrapper<>(questionDiscussionThumb);
        QuestionDiscussionThumb oldThumb = this.getOne(thumbQueryWrapper);
        boolean result;
        if (oldThumb != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                result = questionDiscussionService.update()
                        .eq("id", discussionId)
                        .gt("thumbNum", 0)
                        .setSql("thumbNum = thumbNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            result = this.save(questionDiscussionThumb);
            if (result) {
                result = questionDiscussionService.update()
                        .eq("id", discussionId)
                        .setSql("thumbNum = thumbNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }
}
