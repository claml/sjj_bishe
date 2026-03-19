package com.sjj.oj_backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Contest view object
 */
@Data
public class ContestVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * title
     */
    private String title;

    /**
     * description
     */
    private String description;

    /**
     * start time
     */
    private Date startTime;

    /**
     * end time
     */
    private Date endTime;

    /**
     * creator user id
     */
    private Long userId;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;

    /**
     * contest status: not_started / running / ended
     */
    private String status;

    /**
     * whether current user has joined
     */
    private Boolean joined;

    /**
     * contest questions
     */
    private List<QuestionVO> questionList;

    /**
     * solved question ids for current user in this contest
     */
    private List<Long> solvedQuestionIdList;

    private static final long serialVersionUID = 1L;
}


