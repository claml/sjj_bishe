package com.sjj.oj_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Question submit
 */
@TableName(value = "question_submit")
@Data
public class QuestionSubmit {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * language
     */
    private String language;

    /**
     * user code
     */
    private String code;

    /**
     * judge info (json)
     */
    private String judgeInfo;

    /**
     * status (0 waiting, 1 running, 2 judged, 3 failed)
     */
    private Integer status;

    /**
     * question id
     */
    private Long questionId;

    /**
     * contest id (nullable)
     */
    private Long contestId;

    /**
     * submit user id
     */
    private Long userId;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;

    /**
     * logical delete
     */
    private Integer isDelete;
}


