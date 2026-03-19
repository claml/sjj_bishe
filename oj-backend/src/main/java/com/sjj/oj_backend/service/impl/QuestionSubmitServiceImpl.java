package com.sjj.oj_backend.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.constant.CommonConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.judge.JudgeService;
import com.sjj.oj_backend.judge.codesandbox.model.JudgeInfo;
import com.sjj.oj_backend.mapper.ContestMapper;
import com.sjj.oj_backend.mapper.QuestionMapper;
import com.sjj.oj_backend.mapper.QuestionSubmitMapper;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.sjj.oj_backend.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.sjj.oj_backend.model.entity.Contest;
import com.sjj.oj_backend.model.entity.ContestQuestion;
import com.sjj.oj_backend.model.entity.ContestUser;
import com.sjj.oj_backend.model.entity.Question;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.enums.QuestionSubmitLanguageEnum;
import com.sjj.oj_backend.model.enums.QuestionSubmitStatusEnum;
import com.sjj.oj_backend.model.vo.AcceptedQuestionSubmitVO;
import com.sjj.oj_backend.model.vo.QuestionVO;
import com.sjj.oj_backend.model.vo.QuestionSubmitVO;
import com.sjj.oj_backend.model.vo.UserVO;
import com.sjj.oj_backend.service.ContestQuestionService;
import com.sjj.oj_backend.service.ContestUserService;
import com.sjj.oj_backend.service.QuestionService;
import com.sjj.oj_backend.service.QuestionSubmitService;
import com.sjj.oj_backend.service.UserService;
import com.sjj.oj_backend.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private ContestMapper contestMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private ContestUserService contestUserService;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    @Lazy
    private JudgeService judgeService;

    /**
     * Submit question
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid language");
        }

        long questionId = questionSubmitAddRequest.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Question not found");
        }

        long userId = loginUser.getId();
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        JudgeInfo waitingJudgeInfo = new JudgeInfo();
        waitingJudgeInfo.setMessage("Waiting");
        waitingJudgeInfo.setOutputList(new ArrayList<>());
        questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(waitingJudgeInfo));

        Long contestId = questionSubmitAddRequest.getContestId();
        if (contestId != null) {
            if (contestId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid contest id");
            }
            validateContestSubmit(contestId, questionId, userId);
            questionSubmit.setContestId(contestId);
        }

        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Create submit failed");
        }
        int increaseResult = questionMapper.increaseSubmitNum(questionId);
        if (increaseResult <= 0) {
            log.error("Failed to increase question submit count, questionId={}", questionId);
        }

        Long questionSubmitId = questionSubmit.getId();
        CompletableFuture.runAsync(() -> {
            try {
                judgeService.doJudge(questionSubmitId);
            } catch (Exception e) {
                log.error("Judge async task failed, questionSubmitId={}", questionSubmitId, e);
                QuestionSubmit submitUpdate = new QuestionSubmit();
                submitUpdate.setId(questionSubmitId);
                submitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                JudgeInfo judgeInfo = new JudgeInfo();
                judgeInfo.setMessage("System Error");
                judgeInfo.setTime(0L);
                judgeInfo.setMemory(0L);
                judgeInfo.setOutputList(new ArrayList<>());
                submitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
                this.updateById(submitUpdate);
            }
        });
        return questionSubmitId;
    }

    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long id = questionSubmitQueryRequest.getId();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        Long contestId = questionSubmitQueryRequest.getContestId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(contestId), "contestId", contestId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        long userId = loginUser.getId();
        if (userId != questionSubmit.getUserId() && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(
                questionSubmitPage.getCurrent(),
                questionSubmitPage.getSize(),
                questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }

        Set<Long> questionIdSet = questionSubmitList.stream()
                .map(QuestionSubmit::getQuestionId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, Question> questionIdQuestionMap = questionIdSet.isEmpty()
                ? new HashMap<>()
                : questionService.listByIds(questionIdSet).stream()
                .collect(Collectors.toMap(Question::getId, item -> item, (a, b) -> a));

        Set<Long> userIdSet = questionSubmitList.stream()
                .map(QuestionSubmit::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, User> userIdUserMap = userIdSet.isEmpty()
                ? new HashMap<>()
                : userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, item -> item, (a, b) -> a));

        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> {
                    QuestionSubmitVO questionSubmitVO = getQuestionSubmitVO(questionSubmit, loginUser);

                    Question question = questionIdQuestionMap.get(questionSubmit.getQuestionId());
                    if (question != null) {
                        QuestionVO questionVO = questionService.getQuestionVO(question, null);
                        questionSubmitVO.setQuestionVO(questionVO);
                    }

                    User submitUser = userIdUserMap.get(questionSubmit.getUserId());
                    UserVO userVO = userService.getUserVO(submitUser);
                    questionSubmitVO.setUserVO(userVO);
                    return questionSubmitVO;
                })
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }

    @Override
    public Page<AcceptedQuestionSubmitVO> listAcceptedQuestionByUserId(long userId, long current, long pageSize) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safeCurrent - 1) * safePageSize;

        long total = baseMapper.countDistinctAcceptedQuestionByUserId(userId);
        List<AcceptedQuestionSubmitVO> records = baseMapper.listLatestAcceptedQuestionByUserId(userId, offset, safePageSize);

        Page<AcceptedQuestionSubmitVO> page = new Page<>(safeCurrent, safePageSize, total);
        page.setRecords(records);
        return page;
    }

    /**
     * Validate contest submit constraints
     */
    private void validateContestSubmit(Long contestId, Long questionId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Contest not found");
        }

        Date now = new Date();
        if (contest.getStartTime() != null && now.before(contest.getStartTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Contest is not started");
        }
        if (contest.getEndTime() != null && now.after(contest.getEndTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Contest is ended");
        }

        QueryWrapper<ContestUser> joinWrapper = new QueryWrapper<>();
        joinWrapper.eq("contestId", contestId);
        joinWrapper.eq("userId", userId);
        long joinCount = contestUserService.count(joinWrapper);
        if (joinCount <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "User has not joined this contest");
        }

        QueryWrapper<ContestQuestion> contestQuestionWrapper = new QueryWrapper<>();
        contestQuestionWrapper.eq("contestId", contestId);
        contestQuestionWrapper.eq("questionId", questionId);
        contestQuestionWrapper.eq("isDelete", false);
        long questionCount = contestQuestionService.count(contestQuestionWrapper);
        if (questionCount <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Question does not belong to contest");
        }
    }
}

