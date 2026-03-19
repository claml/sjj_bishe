package com.sjj.oj_backend.model.dto.contest;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Contest query request
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContestQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * title
     */
    private String title;

    /**
     * creator user id
     */
    private Long userId;

    /**
     * status filter: not_started / running / ended
     */
    private String status;

    private static final long serialVersionUID = 1L;
}


