package com.sjj.oj_backend.job.cycle;

import com.sjj.oj_backend.service.PostEsSyncService;
import java.util.Date;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 增量同步帖子到 es
 *
 * 
 * 
 */
// todo 取消注释开启任务
//@Component
@Slf4j
public class IncSyncPostToEs {

    @Resource
    private PostEsSyncService postEsSyncService;

    /**
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 查询近 5 分钟内的数据
        Date fiveMinutesAgoDate = new Date(new Date().getTime() - 5 * 60 * 1000L);
        int total = postEsSyncService.syncByMinUpdateTime(fiveMinutesAgoDate);
        log.info("IncSyncPostToEs end, total {}", total);
    }
}
