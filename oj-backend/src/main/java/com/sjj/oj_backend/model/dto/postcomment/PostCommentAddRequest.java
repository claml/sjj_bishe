package com.sjj.oj_backend.model.dto.postcomment;

import java.io.Serializable;
import lombok.Data;

@Data
public class PostCommentAddRequest implements Serializable {

    private Long postId;

    private String content;

    private static final long serialVersionUID = 1L;
}
