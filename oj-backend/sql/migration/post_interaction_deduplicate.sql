-- 帖子互动（点赞/收藏）数据一致性护栏迁移脚本
-- 目标：
-- 1) 排查历史重复关系
-- 2) 清理历史重复关系（同一 postId + userId 仅保留 1 条）
-- 3) 补齐唯一索引，防止重复关系继续写入
-- 4) 检查 post.thumbNum / favourNum 与关系表计数漂移
-- 5) 修复 post.thumbNum / favourNum 漂移
--
-- 适用库：MySQL 8.x（使用窗口函数）
-- 执行前建议：
-- - 先在从库 / 测试环境演练
-- - 在业务低峰执行
-- - 提前备份 post / post_thumb / post_favour

use my_db;

-- ============================================================
-- 0) 基线信息（可选）
-- ============================================================
select 'post_thumb total' as metric, count(*) as cnt from post_thumb
union all
select 'post_favour total' as metric, count(*) as cnt from post_favour;

-- ============================================================
-- 1) 历史重复数据排查 SQL
-- ============================================================
-- 1.1 点赞重复关系（同一 postId + userId 出现多条）
select postId,
       userId,
       count(*) as duplicate_count,
       min(id)  as keep_id,
       group_concat(id order by id asc) as all_ids
from post_thumb
group by postId, userId
having count(*) > 1
order by duplicate_count desc, postId asc, userId asc;

-- 1.2 收藏重复关系（同一 postId + userId 出现多条）
select postId,
       userId,
       count(*) as duplicate_count,
       min(id)  as keep_id,
       group_concat(id order by id asc) as all_ids
from post_favour
group by postId, userId
having count(*) > 1
order by duplicate_count desc, postId asc, userId asc;

-- ============================================================
-- 2) 历史重复数据清理 SQL
-- 策略：按 id 升序保留最早 1 条，删除其余重复行
-- ============================================================

-- 2.1 清理点赞重复
with ranked_thumb as (
    select id,
           row_number() over (partition by postId, userId order by id asc) as rn
    from post_thumb
)
delete pt
from post_thumb pt
join ranked_thumb rt on pt.id = rt.id
where rt.rn > 1;

-- 2.2 清理收藏重复
with ranked_favour as (
    select id,
           row_number() over (partition by postId, userId order by id asc) as rn
    from post_favour
)
delete pf
from post_favour pf
join ranked_favour rf on pf.id = rf.id
where rf.rn > 1;

-- 2.3 清理后复核（应返回 0 行）
select postId, userId, count(*) as duplicate_count
from post_thumb
group by postId, userId
having count(*) > 1;

select postId, userId, count(*) as duplicate_count
from post_favour
group by postId, userId
having count(*) > 1;

-- ============================================================
-- 3) 补齐唯一索引（数据库兜底）
-- ============================================================
-- 若历史已存在同名索引，请先 drop 后再 add（按实际库结构执行）
alter table post_thumb
    add unique key uk_post_user (postId, userId);

alter table post_favour
    add unique key uk_post_user (postId, userId);

-- ============================================================
-- 4) 点赞/收藏计数字段漂移检查 SQL
-- ============================================================
-- 4.1 漂移明细
select p.id,
       p.thumbNum as stored_thumb_num,
       coalesce(t.real_thumb_num, 0) as actual_thumb_num,
       p.favourNum as stored_favour_num,
       coalesce(f.real_favour_num, 0) as actual_favour_num
from post p
left join (
    select postId, count(*) as real_thumb_num
    from post_thumb
    group by postId
) t on p.id = t.postId
left join (
    select postId, count(*) as real_favour_num
    from post_favour
    group by postId
) f on p.id = f.postId
where p.thumbNum <> coalesce(t.real_thumb_num, 0)
   or p.favourNum <> coalesce(f.real_favour_num, 0)
order by p.id asc;

-- 4.2 漂移聚合统计
select count(*) as drift_post_count
from post p
left join (
    select postId, count(*) as real_thumb_num
    from post_thumb
    group by postId
) t on p.id = t.postId
left join (
    select postId, count(*) as real_favour_num
    from post_favour
    group by postId
) f on p.id = f.postId
where p.thumbNum <> coalesce(t.real_thumb_num, 0)
   or p.favourNum <> coalesce(f.real_favour_num, 0);

-- ============================================================
-- 5) 点赞/收藏计数修复 SQL
-- ============================================================
update post p
left join (
    select postId, count(*) as real_thumb_num
    from post_thumb
    group by postId
) t on p.id = t.postId
left join (
    select postId, count(*) as real_favour_num
    from post_favour
    group by postId
) f on p.id = f.postId
set p.thumbNum = coalesce(t.real_thumb_num, 0),
    p.favourNum = coalesce(f.real_favour_num, 0)
where p.thumbNum <> coalesce(t.real_thumb_num, 0)
   or p.favourNum <> coalesce(f.real_favour_num, 0);

-- 5.1 修复后复核（理论上应为 0）
select count(*) as drift_post_count_after_fix
from post p
left join (
    select postId, count(*) as real_thumb_num
    from post_thumb
    group by postId
) t on p.id = t.postId
left join (
    select postId, count(*) as real_favour_num
    from post_favour
    group by postId
) f on p.id = f.postId
where p.thumbNum <> coalesce(t.real_thumb_num, 0)
   or p.favourNum <> coalesce(f.real_favour_num, 0);
