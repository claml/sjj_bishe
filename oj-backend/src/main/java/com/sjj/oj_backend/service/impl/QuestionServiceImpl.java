package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.constant.CommonConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.mapper.QuestionMapper;
import com.sjj.oj_backend.mapper.QuestionSubmitMapper;
import com.sjj.oj_backend.model.dto.question.QuestionQueryRequest;
import com.sjj.oj_backend.model.entity.Question;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.enums.QuestionSubmitStatusEnum;
import com.sjj.oj_backend.model.vo.QuestionVO;
import com.sjj.oj_backend.service.QuestionService;
import com.sjj.oj_backend.service.UserService;
import com.sjj.oj_backend.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionSubmitMapper questionSubmitMapper;

    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "title too long");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "content too long");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "answer too long");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "judge case too long");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "judge config too long");
        }
    }

    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        String title = questionQueryRequest.getTitle();

        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        boolean validSortField = SqlUtils.validSortField(sortField);
        boolean asc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
        queryWrapper.orderBy(validSortField, asc, sortField);
        if (validSortField && "createTime".equals(sortField)) {
            queryWrapper.orderBy(true, asc, "id");
        }
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        questionVO.setAnswer(null);
        if (canViewQuestionAnswer(question.getId(), request)) {
            questionVO.setAnswer(question.getAnswer());
        }

        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        questionVO.setUserVO(userService.getUserVO(user));
        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }

        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            questionVO.setAnswer(null);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userService.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    private boolean canViewQuestionAnswer(Long questionId, HttpServletRequest request) {
        if (questionId == null || questionId <= 0 || request == null) {
            return false;
        }
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser == null) {
            return false;
        }
        if (userService.isAdmin(loginUser)) {
            return true;
        }
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionId", questionId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        queryWrapper.eq("isDelete", false);
        return questionSubmitMapper.selectCount(queryWrapper) > 0;
    }
}
