package com.sjj.oj_backend.judge.codesandbox.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.sjj.oj_backend.judge.codesandbox.CodeSandbox;
import com.sjj.oj_backend.judge.codesandbox.model.ExecuteCodeRequest;
import com.sjj.oj_backend.judge.codesandbox.model.ExecuteCodeResponse;
import com.sjj.oj_backend.judge.codesandbox.model.JudgeInfo;
import com.sjj.oj_backend.model.enums.QuestionSubmitStatusEnum;

public class RemoteCodeSandbox implements CodeSandbox {

    private static final String DEFAULT_URL = "http://127.0.0.1:8090/codesandbox/run";
    private static final String DEFAULT_AUTH_HEADER = "oj-codesandbox-auth-by-sjj";
    private static final String DEFAULT_AUTH_SECRET = "$W$~vrZwe7z&L!ht^U%fF2zZzHTjWSwY%@ZeEJ^*(qZ()D3npx";

    private final String sandboxUrl;
    private final String authHeaderName;
    private final String authSecret;

    public RemoteCodeSandbox() {
        this(DEFAULT_URL, DEFAULT_AUTH_HEADER, DEFAULT_AUTH_SECRET);
    }

    public RemoteCodeSandbox(String sandboxUrl, String authHeaderName, String authSecret) {
        this.sandboxUrl = StrUtil.blankToDefault(sandboxUrl, DEFAULT_URL);
        this.authHeaderName = StrUtil.blankToDefault(authHeaderName, DEFAULT_AUTH_HEADER);
        this.authSecret = StrUtil.blankToDefault(authSecret, DEFAULT_AUTH_SECRET);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        try (HttpResponse httpResponse = HttpRequest.post(sandboxUrl)
                .header(authHeaderName, authSecret)
                .body(JSONUtil.toJsonStr(executeCodeRequest), "application/json")
                .timeout(30_000)
                .execute()) {

            if (!httpResponse.isOk()) {
                return buildFailedResponse("Remote sandbox HTTP error: " + httpResponse.getStatus());
            }
            String body = httpResponse.body();
            if (StrUtil.isBlank(body)) {
                return buildFailedResponse("Remote sandbox returned empty response");
            }
            return JSONUtil.toBean(body, ExecuteCodeResponse.class);
        } catch (Exception e) {
            return buildFailedResponse("Remote sandbox call failed: " + e.getMessage());
        }
    }

    private ExecuteCodeResponse buildFailedResponse(String message) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        response.setMessage(message);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message);
        response.setJudgeInfo(judgeInfo);
        return response;
    }
}
