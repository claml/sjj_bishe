package com.sjj.oj_backend.model.dto.questionsubmit;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Query accepted submissions by user.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionAcceptedQueryRequest extends PageRequest implements Serializable {

    /**
     * User id.
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
