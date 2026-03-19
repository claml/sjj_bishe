package com.sjj.oj_backend.model.dto.contest;

import lombok.Data;

import java.io.Serializable;

/**
 * Contest update request
 */
@Data
public class ContestUpdateRequest implements Serializable {

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
     * start time (yyyy-MM-dd HH:mm:ss)
     */
    private String startTime;

    /**
     * end time (yyyy-MM-dd HH:mm:ss)
     */
    private String endTime;

    private static final long serialVersionUID = 1L;
}


