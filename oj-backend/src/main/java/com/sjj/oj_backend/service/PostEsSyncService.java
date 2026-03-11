package com.sjj.oj_backend.service;

import java.util.Date;

/**
 * 帖子 ES 同步服务
 */
public interface PostEsSyncService {

    /**
     * 全量同步
     *
     * @return 同步数据量
     */
    int syncAll();

    /**
     * 增量同步（按更新时间）
     *
     * @param minUpdateTime 最小更新时间
     * @return 同步数据量
     */
    int syncByMinUpdateTime(Date minUpdateTime);
}
