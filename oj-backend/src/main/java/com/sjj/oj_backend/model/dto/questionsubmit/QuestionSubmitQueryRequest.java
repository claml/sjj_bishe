package com.sjj.oj_backend.model.dto.questionsubmit;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Query request
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * submit id
     */
    private Long id;

    /**
     * language
     */
    private String language;

    /**
     * submit status
     */
    private Integer status;

    /**
     * question id
     */
    private Long questionId;

    /**
     * user id
     */
    private Long userId;

    /**
     * contest id
     */
    private Long contestId;

    private static final long serialVersionUID = 1L;
}


