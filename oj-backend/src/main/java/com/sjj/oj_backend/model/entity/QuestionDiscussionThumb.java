package com.sjj.oj_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * question discussion thumb
 */
@TableName(value = "question_discussion_thumb")
@Data
public class QuestionDiscussionThumb implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long discussionId;

    private Long userId;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
