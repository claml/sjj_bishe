package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.mapper.QuestionDiscussionMapper;
import com.sjj.oj_backend.mapper.QuestionDiscussionThumbMapper;
import com.sjj.oj_backend.model.dto.questiondiscussion.QuestionDiscussionQueryRequest;
import com.sjj.oj_backend.model.entity.Question;
import com.sjj.oj_backend.model.entity.QuestionDiscussion;
import com.sjj.oj_backend.model.entity.QuestionDiscussionThumb;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.QuestionDiscussionVO;
import com.sjj.oj_backend.service.QuestionDiscussionService;
import com.sjj.oj_backend.service.QuestionService;
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
public class QuestionDiscussionServiceImpl extends ServiceImpl<QuestionDiscussionMapper, QuestionDiscussion>
        implements QuestionDiscussionService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionDiscussionThumbMapper questionDiscussionThumbMapper;

    @Resource
    private UserService userService;

    @Override
    public void validQuestionDiscussion(QuestionDiscussion questionDiscussion, boolean add) {
        ThrowUtils.throwIf(questionDiscussion == null, ErrorCode.PARAMS_ERROR);
        if (add) {
            ThrowUtils.throwIf(questionDiscussion.getQuestionId() == null || questionDiscussion.getQuestionId() <= 0,
                    ErrorCode.PARAMS_ERROR, "question not found");
            ThrowUtils.throwIf(StringUtils.isBlank(questionDiscussion.getContent()), ErrorCode.PARAMS_ERROR,
                    "content can not be empty");
        }
        if (StringUtils.isNotBlank(questionDiscussion.getContent()) && questionDiscussion.getContent().length() > 2000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "content too long");
        }
    }

    @Override
    public Long addQuestionDiscussion(Long questionId, String content, User loginUser) {
        Question question = questionService.getById(questionId);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "question not found");
        QuestionDiscussion questionDiscussion = new QuestionDiscussion();
        questionDiscussion.setQuestionId(questionId);
        questionDiscussion.setContent(content);
        questionDiscussion.setUserId(loginUser.getId());
        questionDiscussion.setThumbNum(0);
        validQuestionDiscussion(questionDiscussion, true);
        boolean result = this.save(questionDiscussion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "add discussion failed");
        return questionDiscussion.getId();
    }

    @Override
    public boolean deleteQuestionDiscussion(Long discussionId, User loginUser, boolean admin) {
        ThrowUtils.throwIf(discussionId == null || discussionId <= 0, ErrorCode.PARAMS_ERROR);
        QuestionDiscussion oldDiscussion = this.getById(discussionId);
        ThrowUtils.throwIf(oldDiscussion == null, ErrorCode.NOT_FOUND_ERROR);
        if (!admin && !oldDiscussion.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = this.removeById(discussionId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "delete discussion failed");
        QueryWrapper<QuestionDiscussionThumb> thumbQueryWrapper = new QueryWrapper<>();
        thumbQueryWrapper.eq("discussionId", discussionId);
        questionDiscussionThumbMapper.delete(thumbQueryWrapper);
        return true;
    }

    @Override
    public Page<QuestionDiscussionVO> listQuestionDiscussionVOByPage(
            QuestionDiscussionQueryRequest questionDiscussionQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(questionDiscussionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = questionDiscussionQueryRequest.getCurrent();
        long pageSize = questionDiscussionQueryRequest.getPageSize();
        Long questionId = questionDiscussionQueryRequest.getQuestionId();
        ThrowUtils.throwIf(questionId == null || questionId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR);

        QueryWrapper<QuestionDiscussion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionId", questionId);
        queryWrapper.orderByDesc("thumbNum");
        queryWrapper.orderByDesc("createTime");
        Page<QuestionDiscussion> page = this.page(new Page<>(current, pageSize), queryWrapper);

        Page<QuestionDiscussionVO> voPage = new Page<>(current, pageSize, page.getTotal());
        voPage.setRecords(getQuestionDiscussionVOList(page.getRecords(), request));
        return voPage;
    }

    @Override
    public List<QuestionDiscussionVO> getQuestionDiscussionVOList(List<QuestionDiscussion> questionDiscussionList,
            HttpServletRequest request) {
        if (questionDiscussionList == null || questionDiscussionList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIdSet = questionDiscussionList.stream().map(QuestionDiscussion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        Map<Long, Boolean> discussionIdHasThumbMap = Collections.emptyMap();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> discussionIdSet = questionDiscussionList.stream()
                    .map(QuestionDiscussion::getId)
                    .collect(Collectors.toSet());
            QueryWrapper<QuestionDiscussionThumb> thumbQueryWrapper = new QueryWrapper<>();
            thumbQueryWrapper.in("discussionId", discussionIdSet);
            thumbQueryWrapper.eq("userId", loginUser.getId());
            discussionIdHasThumbMap = questionDiscussionThumbMapper.selectList(thumbQueryWrapper)
                    .stream()
                    .collect(Collectors.toMap(QuestionDiscussionThumb::getDiscussionId, thumb -> true, (a, b) -> a));
        }
        Map<Long, Boolean> finalDiscussionIdHasThumbMap = discussionIdHasThumbMap;

        return questionDiscussionList.stream().map(discussion -> {
            QuestionDiscussionVO vo = new QuestionDiscussionVO();
            BeanUtils.copyProperties(discussion, vo);
            User user = userIdUserListMap
                    .getOrDefault(discussion.getUserId(), Collections.emptyList())
                    .stream()
                    .findFirst()
                    .orElse(null);
            vo.setUser(userService.getUserVO(user));
            vo.setHasThumb(finalDiscussionIdHasThumbMap.getOrDefault(discussion.getId(), false));
            return vo;
        }).collect(Collectors.toList());
    }
}
