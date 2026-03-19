package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.mapper.ContestQuestionMapper;
import com.sjj.oj_backend.model.entity.ContestQuestion;
import com.sjj.oj_backend.service.ContestQuestionService;
import org.springframework.stereotype.Service;

/**
 * Contest question service implementation
 */
@Service
public class ContestQuestionServiceImpl extends ServiceImpl<ContestQuestionMapper, ContestQuestion>
        implements ContestQuestionService {
}


