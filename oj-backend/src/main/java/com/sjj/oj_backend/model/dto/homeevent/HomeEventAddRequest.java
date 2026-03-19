package com.sjj.oj_backend.model.dto.homeevent;

import lombok.Data;

import java.io.Serializable;

/**
 * Add home event request.
 */
@Data
public class HomeEventAddRequest implements Serializable {

    /**
     * Event type
     */
    private String contestType;

    /**
     * Title
     */
    private String title;

    /**
     * Event time, format yyyy-MM-dd HH:mm:ss
     */
    private String eventTime;

    /**
     * Location
     */
    private String location;

    /**
     * Optional url
     */
    private String eventUrl;

    /**
     * Description
     */
    private String description;

    /**
     * Sort order
     */
    private Integer sortOrder;

    private static final long serialVersionUID = 1L;
}
