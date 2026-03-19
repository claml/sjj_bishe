package com.sjj.oj_backend.model.dto.questiondiscussion;

import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionDiscussionAddRequest implements Serializable {

    private Long questionId;

    private String content;

    private static final long serialVersionUID = 1L;
}
