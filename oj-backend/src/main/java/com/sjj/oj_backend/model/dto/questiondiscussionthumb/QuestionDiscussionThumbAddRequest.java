package com.sjj.oj_backend.model.dto.questiondiscussionthumb;

import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionDiscussionThumbAddRequest implements Serializable {

    private Long discussionId;

    private static final long serialVersionUID = 1L;
}
