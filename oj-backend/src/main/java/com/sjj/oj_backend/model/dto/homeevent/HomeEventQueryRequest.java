package com.sjj.oj_backend.model.dto.homeevent;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Query home events request.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HomeEventQueryRequest extends PageRequest implements Serializable {

    /**
     * Title fuzzy match
     */
    private String title;

    /**
     * Exact event type match
     */
    private String contestType;

    private static final long serialVersionUID = 1L;
}
