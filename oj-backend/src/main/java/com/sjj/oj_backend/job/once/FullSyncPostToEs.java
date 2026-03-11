package com.sjj.oj_backend.job.once;

import com.sjj.oj_backend.service.PostEsSyncService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * 全量同步帖子到 es
 *
 * 
 * 
 */
// todo 取消注释开启任务
//@Component
@Slf4j
public class FullSyncPostToEs implements CommandLineRunner {

    @Resource
    private PostEsSyncService postEsSyncService;

    @Override
    public void run(String... args) {
        int total = postEsSyncService.syncAll();
        log.info("FullSyncPostToEs end, total {}", total);
    }
}
