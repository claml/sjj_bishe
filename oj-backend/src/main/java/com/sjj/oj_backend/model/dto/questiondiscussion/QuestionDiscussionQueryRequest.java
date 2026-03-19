package com.sjj.oj_backend.model.dto.questiondiscussion;

import com.sjj.oj_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionDiscussionQueryRequest extends PageRequest {

    private Long questionId;
}
