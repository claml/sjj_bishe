package com.sjj.oj_backend.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class PostCommentVO implements Serializable {

    private Long id;

    private Long postId;

    private String content;

    private Long userId;

    private Date createTime;

    private Date updateTime;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}
