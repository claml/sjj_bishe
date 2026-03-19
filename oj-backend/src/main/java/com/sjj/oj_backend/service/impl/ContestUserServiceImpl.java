package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.mapper.ContestUserMapper;
import com.sjj.oj_backend.model.entity.ContestUser;
import com.sjj.oj_backend.service.ContestUserService;
import org.springframework.stereotype.Service;

/**
 * Contest participant service implementation
 */
@Service
public class ContestUserServiceImpl extends ServiceImpl<ContestUserMapper, ContestUser>
        implements ContestUserService {
}


