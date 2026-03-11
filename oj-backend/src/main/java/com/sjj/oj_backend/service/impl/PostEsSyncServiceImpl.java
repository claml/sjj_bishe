package com.sjj.oj_backend.service.impl;

import com.sjj.oj_backend.esdao.PostEsDao;
import com.sjj.oj_backend.mapper.PostMapper;
import com.sjj.oj_backend.model.dto.post.PostEsDTO;
import com.sjj.oj_backend.model.entity.Post;
import com.sjj.oj_backend.service.PostEsSyncService;
import com.sjj.oj_backend.service.PostService;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.collection.CollUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * 帖子 ES 同步服务实现
 */
@Service
@ConditionalOnBean(PostEsDao.class)
@Slf4j
public class PostEsSyncServiceImpl implements PostEsSyncService {

    @Resource
    private PostService postService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostEsDao postEsDao;

    private static final int SYNC_PAGE_SIZE = 500;

    @Override
    public int syncAll() {
        List<Post> postList = postService.list();
        return syncPostList(postList, "full");
    }

    @Override
    public int syncByMinUpdateTime(Date minUpdateTime) {
        List<Post> postList = postMapper.listPostWithDelete(minUpdateTime);
        return syncPostList(postList, "inc");
    }

    private int syncPostList(List<Post> postList, String syncType) {
        if (CollUtil.isEmpty(postList)) {
            log.info("{} sync post skipped, empty data", syncType);
            return 0;
        }
        List<PostEsDTO> postEsDTOList = postList.stream().map(PostEsDTO::objToDto).collect(Collectors.toList());
        int total = postEsDTOList.size();
        log.info("{} sync post start, total={}", syncType, total);
        for (int i = 0; i < total; i += SYNC_PAGE_SIZE) {
            int end = Math.min(i + SYNC_PAGE_SIZE, total);
            postEsDao.saveAll(postEsDTOList.subList(i, end));
            log.info("{} sync post from {} to {}", syncType, i, end);
        }
        log.info("{} sync post end, total={}", syncType, total);
        return total;
    }
}
