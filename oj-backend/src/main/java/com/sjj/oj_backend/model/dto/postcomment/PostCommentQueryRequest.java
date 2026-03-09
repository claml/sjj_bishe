package com.sjj.oj_backend.model.dto.postcomment;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostCommentQueryRequest extends PageRequest {

    private Long postId;
}
