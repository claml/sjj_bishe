package com.sjj.oj_backend.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.constant.CommonConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.mapper.ContestMapper;
import com.sjj.oj_backend.model.dto.contest.ContestAddRequest;
import com.sjj.oj_backend.model.dto.contest.ContestQueryRequest;
import com.sjj.oj_backend.model.dto.contest.ContestUpdateRequest;
import com.sjj.oj_backend.model.dto.question.JudgeCase;
import com.sjj.oj_backend.model.dto.question.JudgeConfig;
import com.sjj.oj_backend.model.dto.question.QuestionAddRequest;
import com.sjj.oj_backend.model.entity.Contest;
import com.sjj.oj_backend.model.entity.ContestQuestion;
import com.sjj.oj_backend.model.entity.ContestUser;
import com.sjj.oj_backend.model.entity.Question;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.enums.JudgeInfoMessageEnum;
import com.sjj.oj_backend.model.enums.QuestionSubmitStatusEnum;
import com.sjj.oj_backend.model.vo.ContestRankVO;
import com.sjj.oj_backend.model.vo.ContestVO;
import com.sjj.oj_backend.model.vo.QuestionVO;
import com.sjj.oj_backend.service.ContestQuestionService;
import com.sjj.oj_backend.service.ContestService;
import com.sjj.oj_backend.service.ContestUserService;
import com.sjj.oj_backend.service.QuestionService;
import com.sjj.oj_backend.service.QuestionSubmitService;
import com.sjj.oj_backend.service.UserService;
import com.sjj.oj_backend.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contest service implementation
 */
@Service
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest> implements ContestService {

    private static final Gson GSON = new Gson();
    /**
     * Contest status priority for list sorting:
     * running(0) -> not_started(1) -> ended(2)
     */
    private static final String CONTEST_STATUS_PRIORITY_ORDER_SQL =
            "CASE " +
                    "WHEN startTime <= NOW() AND endTime >= NOW() THEN 0 " +
                    "WHEN startTime > NOW() THEN 1 " +
                    "ELSE 2 " +
                    "END";

    private static final List<String> DATE_TIME_PATTERNS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
    );

    @Resource
    private QuestionService questionService;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    private ContestUserService contestUserService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    @Override
    public void validContest(Contest contest, boolean add) {
        if (contest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = contest.getTitle();
        String description = contest.getDescription();
        String inviteCode = contest.getInviteCode();
        Date startTime = contest.getStartTime();
        Date endTime = contest.getEndTime();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR, "Contest title is required");
            ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR,
                    "Contest time range is required");
        }
        if (StringUtils.isNotBlank(title) && title.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Contest title is too long");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Contest description is too long");
        }
        if (StringUtils.isNotBlank(inviteCode) && inviteCode.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invite code is too long");
        }
        if (startTime != null && endTime != null && !startTime.before(endTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Contest start time must be before end time");
        }
    }

    @Override
    public QueryWrapper<Contest> getQueryWrapper(ContestQueryRequest contestQueryRequest) {
        QueryWrapper<Contest> queryWrapper = new QueryWrapper<>();
        if (contestQueryRequest == null) {
            queryWrapper.eq("isDelete", false);
            queryWrapper.orderByAsc(CONTEST_STATUS_PRIORITY_ORDER_SQL);
            queryWrapper.orderByDesc("createTime");
            return queryWrapper;
        }
        Long id = contestQueryRequest.getId();
        String title = contestQueryRequest.getTitle();
        Long userId = contestQueryRequest.getUserId();
        String status = contestQueryRequest.getStatus();
        String sortField = contestQueryRequest.getSortField();
        String sortOrder = contestQueryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq("isDelete", false);

        Date now = new Date();
        if (StringUtils.isNotBlank(status)) {
            if ("not_started".equals(status)) {
                queryWrapper.gt("startTime", now);
            } else if ("running".equals(status)) {
                queryWrapper.le("startTime", now);
                queryWrapper.ge("endTime", now);
            } else if ("ended".equals(status)) {
                queryWrapper.lt("endTime", now);
            }
        }
        queryWrapper.orderByAsc(CONTEST_STATUS_PRIORITY_ORDER_SQL);
        if (!SqlUtils.validSortField(sortField)) {
            sortField = "createTime";
        }
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
        queryWrapper.orderBy(true, isAsc, sortField);
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addContest(ContestAddRequest contestAddRequest, User loginUser) {
        if (contestAddRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Contest contest = new Contest();
        contest.setTitle(StringUtils.trimToEmpty(contestAddRequest.getTitle()));
        contest.setDescription(contestAddRequest.getDescription());
        contest.setStartTime(parseDateTime(contestAddRequest.getStartTime(), "startTime"));
        contest.setEndTime(parseDateTime(contestAddRequest.getEndTime(), "endTime"));
        contest.setInviteCode(StringUtils.trimToNull(contestAddRequest.getInviteCode()));
        contest.setUserId(loginUser.getId());
        validContest(contest, true);

        boolean saveContestResult = this.save(contest);
        ThrowUtils.throwIf(!saveContestResult, ErrorCode.OPERATION_ERROR, "Create contest failed");
        Long contestId = contest.getId();

        LinkedHashSet<Long> questionIdSet = new LinkedHashSet<>();

        List<Long> requestQuestionIdList = contestAddRequest.getQuestionIdList();
        if (CollectionUtils.isNotEmpty(requestQuestionIdList)) {
            List<Long> validQuestionIdList = requestQuestionIdList.stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toList());
            ThrowUtils.throwIf(validQuestionIdList.size() != requestQuestionIdList.size(),
                    ErrorCode.PARAMS_ERROR, "Invalid question id");

            List<Question> questionList = questionService.listByIds(validQuestionIdList);
            ThrowUtils.throwIf(questionList.size() != validQuestionIdList.size(),
                    ErrorCode.NOT_FOUND_ERROR, "Some question id does not exist");
            questionIdSet.addAll(validQuestionIdList);
        }

        List<QuestionAddRequest> newQuestionList = contestAddRequest.getNewQuestionList();
        if (CollectionUtils.isNotEmpty(newQuestionList)) {
            for (QuestionAddRequest questionAddRequest : newQuestionList) {
                Question newQuestion = buildQuestionByRequest(questionAddRequest, loginUser.getId());
                questionService.validQuestion(newQuestion, true);
                boolean saveQuestionResult = questionService.save(newQuestion);
                ThrowUtils.throwIf(!saveQuestionResult, ErrorCode.OPERATION_ERROR,
                        "Create contest question failed");
                questionIdSet.add(newQuestion.getId());
            }
        }

        ThrowUtils.throwIf(questionIdSet.isEmpty(), ErrorCode.PARAMS_ERROR,
                "Contest must contain at least one question");

        List<ContestQuestion> contestQuestionList = new ArrayList<>();
        int order = 1;
        for (Long questionId : questionIdSet) {
            ContestQuestion contestQuestion = new ContestQuestion();
            contestQuestion.setContestId(contestId);
            contestQuestion.setQuestionId(questionId);
            contestQuestion.setSortOrder(order++);
            contestQuestionList.add(contestQuestion);
        }

        boolean saveBatchResult = contestQuestionService.saveBatch(contestQuestionList);
        ThrowUtils.throwIf(!saveBatchResult, ErrorCode.OPERATION_ERROR, "Bind contest questions failed");
        return contestId;
    }

    @Override
    public boolean updateContest(ContestUpdateRequest contestUpdateRequest) {
        if (contestUpdateRequest == null || contestUpdateRequest.getId() == null || contestUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Contest oldContest = this.getById(contestUpdateRequest.getId());
        ThrowUtils.throwIf(oldContest == null, ErrorCode.NOT_FOUND_ERROR, "Contest not found");

        Date startTime = oldContest.getStartTime();
        Date endTime = oldContest.getEndTime();
        if (StringUtils.isNotBlank(contestUpdateRequest.getStartTime())) {
            startTime = parseDateTime(contestUpdateRequest.getStartTime(), "startTime");
        }
        if (StringUtils.isNotBlank(contestUpdateRequest.getEndTime())) {
            endTime = parseDateTime(contestUpdateRequest.getEndTime(), "endTime");
        }

        Contest updateContest = new Contest();
        updateContest.setId(contestUpdateRequest.getId());
        updateContest.setTitle(contestUpdateRequest.getTitle());
        updateContest.setDescription(contestUpdateRequest.getDescription());
        updateContest.setStartTime(startTime);
        updateContest.setEndTime(endTime);
        validContest(updateContest, false);

        return this.updateById(updateContest);
    }

    @Override
    public boolean joinContest(Long contestId, String inviteCode, User loginUser) {
        ThrowUtils.throwIf(contestId == null || contestId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

        Contest contest = this.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "Contest not found");

        if (isUserJoined(contestId, loginUser.getId())) {
            return true;
        }

        checkContestAccess(contest, loginUser, inviteCode, false);

        Date now = new Date();
        ThrowUtils.throwIf(contest.getEndTime() != null && now.after(contest.getEndTime()),
                ErrorCode.OPERATION_ERROR, "Contest is already ended");

        ContestUser contestUser = new ContestUser();
        contestUser.setContestId(contestId);
        contestUser.setUserId(loginUser.getId());
        return contestUserService.save(contestUser);
    }

    @Override
    public ContestVO getContestVO(Contest contest, User loginUser, boolean withQuestions, String inviteCode) {
        if (contest == null) {
            return null;
        }

        if (withQuestions) {
            checkContestAccess(contest, loginUser, inviteCode, true);
        }

        ContestVO contestVO = new ContestVO();
        BeanUtils.copyProperties(contest, contestVO);
        contestVO.setStatus(getContestStatus(contest));

        boolean joined = false;
        if (loginUser != null && loginUser.getId() != null) {
            joined = isUserJoined(contest.getId(), loginUser.getId());
        }
        contestVO.setJoined(joined);

        if (!withQuestions) {
            return contestVO;
        }

        QueryWrapper<ContestQuestion> contestQuestionQueryWrapper = new QueryWrapper<>();
        contestQuestionQueryWrapper.eq("contestId", contest.getId());
        contestQuestionQueryWrapper.eq("isDelete", false);
        contestQuestionQueryWrapper.orderByAsc("sortOrder", "id");
        List<ContestQuestion> contestQuestionList = contestQuestionService.list(contestQuestionQueryWrapper);
        if (CollectionUtils.isEmpty(contestQuestionList)) {
            contestVO.setQuestionList(Collections.emptyList());
            contestVO.setSolvedQuestionIdList(Collections.emptyList());
            return contestVO;
        }

        List<Long> questionIdList = contestQuestionList.stream()
                .map(ContestQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<Question> questionList = questionService.listByIds(questionIdList);
        Map<Long, Question> idQuestionMap = questionList.stream()
                .collect(Collectors.toMap(Question::getId, item -> item, (a, b) -> a));

        List<QuestionVO> questionVOList = new ArrayList<>();
        for (Long questionId : questionIdList) {
            Question question = idQuestionMap.get(questionId);
            if (question != null) {
                questionVOList.add(questionService.getQuestionVO(question, null));
            }
        }
        contestVO.setQuestionList(questionVOList);

        if (loginUser == null || loginUser.getId() == null) {
            contestVO.setSolvedQuestionIdList(Collections.emptyList());
            return contestVO;
        }

        QueryWrapper<QuestionSubmit> submitQueryWrapper = new QueryWrapper<>();
        submitQueryWrapper.eq("contestId", contest.getId());
        submitQueryWrapper.eq("userId", loginUser.getId());
        submitQueryWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        submitQueryWrapper.eq("isDelete", false);
        List<QuestionSubmit> submitList = questionSubmitService.list(submitQueryWrapper);
        List<Long> solvedQuestionIdList = submitList.stream()
                .filter(this::isAcceptedSubmit)
                .map(QuestionSubmit::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
        contestVO.setSolvedQuestionIdList(solvedQuestionIdList);
        return contestVO;
    }

    @Override
    public Page<ContestVO> getContestVOPage(Page<Contest> contestPage, User loginUser) {
        List<Contest> contestList = contestPage.getRecords();
        Page<ContestVO> contestVOPage = new Page<>(contestPage.getCurrent(), contestPage.getSize(), contestPage.getTotal());
        if (CollectionUtils.isEmpty(contestList)) {
            return contestVOPage;
        }
        List<ContestVO> contestVOList = contestList.stream()
                .map(contest -> getContestVO(contest, loginUser, false, null))
                .collect(Collectors.toList());
        contestVOPage.setRecords(contestVOList);
        return contestVOPage;
    }

    @Override
    public List<ContestRankVO> getContestRankList(Long contestId) {
        ThrowUtils.throwIf(contestId == null || contestId <= 0, ErrorCode.PARAMS_ERROR);
        Contest contest = this.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "Contest not found");

        QueryWrapper<ContestUser> contestUserQueryWrapper = new QueryWrapper<>();
        contestUserQueryWrapper.eq("contestId", contestId);
        List<ContestUser> contestUserList = contestUserService.list(contestUserQueryWrapper);
        if (CollectionUtils.isEmpty(contestUserList)) {
            return Collections.emptyList();
        }

        List<Long> participantUserIdList = contestUserList.stream()
                .map(ContestUser::getUserId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<QuestionSubmit> submitQueryWrapper = new QueryWrapper<>();
        submitQueryWrapper.eq("contestId", contestId);
        submitQueryWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        submitQueryWrapper.eq("isDelete", false);
        submitQueryWrapper.in("userId", participantUserIdList);
        List<QuestionSubmit> submitList = questionSubmitService.list(submitQueryWrapper);

        Map<Long, Set<Long>> userSolvedQuestionSetMap = new HashMap<>();
        Map<Long, Date> userLastAcceptedTimeMap = new HashMap<>();
        for (QuestionSubmit questionSubmit : submitList) {
            if (!isAcceptedSubmit(questionSubmit)) {
                continue;
            }
            Long userId = questionSubmit.getUserId();
            userSolvedQuestionSetMap.computeIfAbsent(userId, key -> new LinkedHashSet<>())
                    .add(questionSubmit.getQuestionId());

            Date updateTime = questionSubmit.getUpdateTime();
            Date oldTime = userLastAcceptedTimeMap.get(userId);
            if (oldTime == null || (updateTime != null && updateTime.after(oldTime))) {
                userLastAcceptedTimeMap.put(userId, updateTime);
            }
        }

        Map<Long, User> userIdUserMap = userService.listByIds(participantUserIdList).stream()
                .collect(Collectors.toMap(User::getId, item -> item, (a, b) -> a));

        List<ContestRankVO> rankVOList = new ArrayList<>();
        for (Long userId : participantUserIdList) {
            ContestRankVO rankVO = new ContestRankVO();
            rankVO.setUserId(userId);

            Set<Long> solvedSet = userSolvedQuestionSetMap.get(userId);
            rankVO.setSolvedCount(solvedSet == null ? 0 : solvedSet.size());
            rankVO.setLastAcceptedTime(userLastAcceptedTimeMap.get(userId));

            User user = userIdUserMap.get(userId);
            if (user != null) {
                rankVO.setUserName(user.getUserName());
                rankVO.setUserAvatar(user.getUserAvatar());
            }
            rankVOList.add(rankVO);
        }

        rankVOList.sort(Comparator
                .comparing(ContestRankVO::getSolvedCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ContestRankVO::getLastAcceptedTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ContestRankVO::getUserId));

        for (int i = 0; i < rankVOList.size(); i++) {
            rankVOList.get(i).setRank(i + 1);
        }
        return rankVOList;
    }

    @Override
    public String getContestStatus(Contest contest) {
        if (contest == null) {
            return "ended";
        }
        Date now = new Date();
        Date startTime = contest.getStartTime();
        Date endTime = contest.getEndTime();
        if (startTime != null && now.before(startTime)) {
            return "not_started";
        }
        if (endTime != null && now.after(endTime)) {
            return "ended";
        }
        return "running";
    }

    private boolean isUserJoined(Long contestId, Long userId) {
        if (contestId == null || contestId <= 0 || userId == null || userId <= 0) {
            return false;
        }
        QueryWrapper<ContestUser> joinWrapper = new QueryWrapper<>();
        joinWrapper.eq("contestId", contestId);
        joinWrapper.eq("userId", userId);
        return contestUserService.count(joinWrapper) > 0;
    }

    private void checkContestAccess(Contest contest, User loginUser, String inviteCode, boolean allowJoinedBypassInviteCode) {
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Contest not found");
        }
        Long loginUserId = loginUser == null ? null : loginUser.getId();
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isCreator = loginUserId != null && Objects.equals(contest.getUserId(), loginUserId);
        if (isAdmin || isCreator) {
            return;
        }

        String trimmedInviteCode = StringUtils.trimToNull(contest.getInviteCode());
        boolean restrictedByInviteCode = StringUtils.isNotBlank(trimmedInviteCode);
        if (!restrictedByInviteCode) {
            return;
        }

        ThrowUtils.throwIf(loginUserId == null || loginUserId <= 0, ErrorCode.NOT_LOGIN_ERROR,
                "Please login to access this contest");

        if (allowJoinedBypassInviteCode && isUserJoined(contest.getId(), loginUserId)) {
            return;
        }

        String requestInviteCode = StringUtils.trimToNull(inviteCode);
        ThrowUtils.throwIf(StringUtils.isBlank(requestInviteCode), ErrorCode.NO_AUTH_ERROR,
                "Invite code is required");
        ThrowUtils.throwIf(!StringUtils.equals(trimmedInviteCode, requestInviteCode), ErrorCode.NO_AUTH_ERROR,
                "Invite code is incorrect");
    }

    private Question buildQuestionByRequest(QuestionAddRequest questionAddRequest, Long userId) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR, "Question payload is empty");
        Question question = new Question();
        question.setTitle(questionAddRequest.getTitle());
        question.setContent(questionAddRequest.getContent());
        question.setAnswer(questionAddRequest.getAnswer());
        question.setTags(GSON.toJson(questionAddRequest.getTags() == null
                ? Collections.emptyList() : questionAddRequest.getTags()));

        List<JudgeCase> judgeCaseList = questionAddRequest.getJudgeCase();
        question.setJudgeCase(GSON.toJson(judgeCaseList == null ? Collections.emptyList() : judgeCaseList));

        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig == null) {
            judgeConfig = new JudgeConfig();
            judgeConfig.setMemoryLimit(128L);
            judgeConfig.setStackLimit(128L);
            judgeConfig.setTimeLimit(1000L);
        }
        question.setJudgeConfig(GSON.toJson(judgeConfig));
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        question.setThumbNum(0);
        question.setFavourNum(0);
        question.setUserId(userId);
        return question;
    }

    private Date parseDateTime(String dateTimeStr, String fieldName) {
        if (StringUtils.isBlank(dateTimeStr)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " is required");
        }
        for (String pattern : DATE_TIME_PATTERNS) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                simpleDateFormat.setLenient(false);
                return simpleDateFormat.parse(dateTimeStr);
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " format is invalid");
    }

    private boolean isAcceptedSubmit(QuestionSubmit questionSubmit) {
        if (questionSubmit == null || StringUtils.isBlank(questionSubmit.getJudgeInfo())) {
            return false;
        }
        String judgeInfoStr = questionSubmit.getJudgeInfo();
        if (!judgeInfoStr.contains("Accepted")) {
            return false;
        }
        try {
            String message = JSONUtil.parseObj(judgeInfoStr).getStr("message");
            return JudgeInfoMessageEnum.ACCEPTED.getValue().equals(message);
        } catch (Exception e) {
            return false;
        }
    }
}

