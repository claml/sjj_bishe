package com.sjj.oj_backend.model.dto.questionsubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * Create submit request
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * language
     */
    private String language;

    /**
     * user code
     */
    private String code;

    /**
     * question id
     */
    private Long questionId;

    /**
     * contest id (optional)
     */
    private Long contestId;

    private static final long serialVersionUID = 1L;
}


