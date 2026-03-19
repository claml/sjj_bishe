package com.sjj.oj_backend.model.dto.contest;

import com.sjj.oj_backend.model.dto.question.QuestionAddRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Contest create request
 */
@Data
public class ContestAddRequest implements Serializable {

    /**
     * title
     */
    private String title;

    /**
     * description
     */
    private String description;

    /**
     * start time (yyyy-MM-dd HH:mm:ss)
     */
    private String startTime;

    /**
     * end time (yyyy-MM-dd HH:mm:ss)
     */
    private String endTime;

    /**
     * invite code
     */
    private String inviteCode;

    /**
     * existing question ids from bank
     */
    private List<Long> questionIdList;

    /**
     * new questions to create in current request
     */
    private List<QuestionAddRequest> newQuestionList;

    private static final long serialVersionUID = 1L;
}


