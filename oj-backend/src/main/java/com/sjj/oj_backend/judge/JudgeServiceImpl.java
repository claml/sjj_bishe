package com.sjj.oj_backend.judge;

import cn.hutool.json.JSONUtil;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.judge.codesandbox.CodeSandbox;
import com.sjj.oj_backend.judge.codesandbox.CodeSandboxFactory;
import com.sjj.oj_backend.judge.codesandbox.CodeSandboxProxy;
import com.sjj.oj_backend.judge.codesandbox.model.ExecuteCodeRequest;
import com.sjj.oj_backend.judge.codesandbox.model.ExecuteCodeResponse;
import com.sjj.oj_backend.judge.codesandbox.model.JudgeInfo;
import com.sjj.oj_backend.judge.strategy.JudgeContext;
import com.sjj.oj_backend.mapper.QuestionMapper;
import com.sjj.oj_backend.model.dto.question.JudgeCase;
import com.sjj.oj_backend.model.entity.Question;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.sjj.oj_backend.model.enums.JudgeInfoMessageEnum;
import com.sjj.oj_backend.model.enums.QuestionSubmitStatusEnum;
import com.sjj.oj_backend.service.QuestionService;
import com.sjj.oj_backend.service.QuestionSubmitService;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;
    @Value("${codesandbox.remote.url:http://127.0.0.1:8090/codesandbox/run}")
    private String remoteUrl;
    @Value("${codesandbox.remote.auth-header:oj-codesandbox-auth-by-sjj}")
    private String remoteAuthHeader;
    @Value("${codesandbox.remote.auth-secret:$W$~vrZwe7z&L!ht^U%fF2zZzHTjWSwY%@ZeEJ^*(qZ()D3npx}")
    private String remoteAuthSecret;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Submit record not found");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Question not found");
        }

        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Submit is already judging");
        }

        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        JudgeInfo runningJudgeInfo = new JudgeInfo();
        runningJudgeInfo.setMessage("Running");
        runningJudgeInfo.setOutputList(new ArrayList<>());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(runningJudgeInfo));
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update submit status");
        }

        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type, remoteUrl, remoteAuthHeader, remoteAuthSecret);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        if (executeCodeResponse == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Code sandbox returned null response");
        }

        if (!QuestionSubmitStatusEnum.SUCCEED.getValue().equals(executeCodeResponse.getStatus())) {
            JudgeInfo failedJudgeInfo = new JudgeInfo();
            JudgeInfo sandboxJudgeInfo = executeCodeResponse.getJudgeInfo();
            if (sandboxJudgeInfo != null) {
                failedJudgeInfo.setTime(sandboxJudgeInfo.getTime());
                failedJudgeInfo.setMemory(sandboxJudgeInfo.getMemory());
                failedJudgeInfo.setMessage(sandboxJudgeInfo.getMessage());
            }
            List<String> failedOutputList = executeCodeResponse.getOutputList();
            if (failedOutputList == null) {
                failedOutputList = new ArrayList<>();
            }
            failedJudgeInfo.setOutputList(failedOutputList);
            if (StringUtils.isBlank(failedJudgeInfo.getMessage())) {
                failedJudgeInfo.setMessage(StringUtils.defaultIfBlank(executeCodeResponse.getMessage(), "Runtime Error"));
            }
            questionSubmitUpdate = new QuestionSubmit();
            questionSubmitUpdate.setId(questionSubmitId);
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(failedJudgeInfo));
            update = questionSubmitService.updateById(questionSubmitUpdate);
            if (!update) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save judge result");
            }
            return questionSubmitService.getById(questionSubmitId);
        }

        List<String> outputList = executeCodeResponse.getOutputList();
        if (outputList == null) {
            outputList = new ArrayList<>();
        }

        JudgeInfo sandboxJudgeInfo = executeCodeResponse.getJudgeInfo();
        if (sandboxJudgeInfo == null) {
            sandboxJudgeInfo = new JudgeInfo();
        }
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(sandboxJudgeInfo);
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        judgeInfo.setOutputList(outputList);

        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save judge result");
        }
        if (isAcceptedResult(judgeInfo)) {
            int increaseAcceptedResult = questionMapper.increaseAcceptedNum(questionId);
            if (increaseAcceptedResult <= 0) {
                log.error("Failed to increase question accepted count, questionId={}", questionId);
            }
        }

        return questionSubmitService.getById(questionSubmitId);
    }

    private boolean isAcceptedResult(JudgeInfo judgeInfo) {
        if (judgeInfo == null || StringUtils.isBlank(judgeInfo.getMessage())) {
            return false;
        }
        return JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeInfo.getMessage());
    }
}
