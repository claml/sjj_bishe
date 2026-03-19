package com.sjj.oj_backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Latest accepted submission info per question.
 */
@Data
public class AcceptedQuestionSubmitVO implements Serializable {

    /**
     * Submission id.
     */
    private Long submitId;

    /**
     * Question id.
     */
    private Long questionId;

    /**
     * Question title.
     */
    private String questionTitle;

    /**
     * Question no synchronized with question list page.
     */
    private Integer questionNo;

    /**
     * Language.
     */
    private String language;

    /**
     * Latest accepted code.
     */
    private String code;

    /**
     * Accepted submit time.
     */
    private Date submitTime;

    private static final long serialVersionUID = 1L;
}
