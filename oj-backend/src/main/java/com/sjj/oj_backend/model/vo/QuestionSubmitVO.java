package com.sjj.oj_backend.model.vo;

import cn.hutool.json.JSONUtil;
import com.sjj.oj_backend.judge.codesandbox.model.JudgeInfo;
import com.sjj.oj_backend.model.entity.QuestionSubmit;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * 题目提交封装类
 * @TableName question
 */
@Data
public class QuestionSubmitVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;

    /**
     * 判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）
     */
    private Integer status;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 提交用户信息
     */
    private UserVO userVO;

    /**
     * 对应题目信息
     */
    private QuestionVO questionVO;

    /**
     * 包装类转对象
     *
     * @param questionSubmitVO
     * @return
     */
    public static QuestionSubmit voToObj(QuestionSubmitVO questionSubmitVO) {
        if (questionSubmitVO == null) {
            return null;
        }
        QuestionSubmit questionSubmit = new QuestionSubmit();
        BeanUtils.copyProperties(questionSubmitVO, questionSubmit);
        JudgeInfo judgeInfoObj = questionSubmitVO.getJudgeInfo();
        if (judgeInfoObj != null) {
            questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfoObj));
        }
        return questionSubmit;
    }

    /**
     * 对象转包装类
     *
     * @param questionSubmit
     * @return
     */
    public static QuestionSubmitVO objToVo(QuestionSubmit questionSubmit) {
        if (questionSubmit == null) {
            return null;
        }
        QuestionSubmitVO questionSubmitVO = new QuestionSubmitVO();
        BeanUtils.copyProperties(questionSubmit, questionSubmitVO);
        JudgeInfo judgeInfo = new JudgeInfo();
        String judgeInfoStr = questionSubmit.getJudgeInfo();
        if (StringUtils.isNotBlank(judgeInfoStr)) {
            try {
                JudgeInfo parsedJudgeInfo = JSONUtil.toBean(judgeInfoStr, JudgeInfo.class);
                if (parsedJudgeInfo != null) {
                    judgeInfo = parsedJudgeInfo;
                }
            } catch (Exception ignored) {
                // Keep default value for compatibility with malformed historical data.
            }
        }
        if (judgeInfo.getOutputList() == null) {
            judgeInfo.setOutputList(new ArrayList<>());
        }
        if (StringUtils.isBlank(judgeInfo.getMessage())) {
            Integer status = questionSubmitVO.getStatus();
            if (status != null) {
                if (status == 0) {
                    judgeInfo.setMessage("Waiting");
                } else if (status == 1) {
                    judgeInfo.setMessage("Running");
                } else if (status == 3) {
                    judgeInfo.setMessage("Failed");
                }
            }
        }
        questionSubmitVO.setJudgeInfo(judgeInfo);
        return questionSubmitVO;
    }

    private static final long serialVersionUID = 1L;
}
