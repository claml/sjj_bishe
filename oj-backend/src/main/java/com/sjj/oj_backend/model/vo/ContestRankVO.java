package com.sjj.oj_backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Contest rank item
 */
@Data
public class ContestRankVO implements Serializable {

    /**
     * rank number
     */
    private Integer rank;

    /**
     * user id
     */
    private Long userId;

    /**
     * user name
     */
    private String userName;

    /**
     * user avatar
     */
    private String userAvatar;

    /**
     * solved question count
     */
    private Integer solvedCount;

    /**
     * latest accepted time for tie-break
     */
    private Date lastAcceptedTime;

    private static final long serialVersionUID = 1L;
}


