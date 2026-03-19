-- home page events for upcoming competitions
create table if not exists home_event
(
    id          bigint auto_increment comment 'id' primary key,
    contestType varchar(32)                        not null comment 'event type',
    title       varchar(128)                       not null comment 'event title',
    eventTime   datetime                           not null comment 'event time',
    location    varchar(128)                       null comment 'event location',
    eventUrl    varchar(512)                       null comment 'event url',
    description varchar(1024)                      null comment 'event description',
    sortOrder   int      default 0                 not null comment 'sort order',
    userId      bigint                             not null comment 'creator user id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete    tinyint  default 0                 not null comment 'is delete',
    index idx_eventTime (eventTime),
    index idx_contestType (contestType),
    index idx_userId (userId)
) comment 'home page event' collate = utf8mb4_unicode_ci;
