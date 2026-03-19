package com.sjj.oj_backend.model.dto.contest;

import lombok.Data;

import java.io.Serializable;

/**
 * Contest join request
 */
@Data
public class ContestJoinRequest implements Serializable {

    /**
     * contest id
     */
    private Long contestId;

    /**
     * invite code
     */
    private String inviteCode;

    private static final long serialVersionUID = 1L;
}


