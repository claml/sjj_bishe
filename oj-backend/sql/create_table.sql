-- init database
create database if not exists my_db;
use my_db;

-- user
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment 'account',
    userPassword varchar(512)                           not null comment 'password',
    unionId      varchar(256)                           null comment 'wx union id',
    mpOpenId     varchar(256)                           null comment 'wx mp open id',
    userName     varchar(256)                           null comment 'user name',
    userAvatar   varchar(1024)                          null comment 'avatar',
    userProfile  varchar(512)                           null comment 'profile',
    userRole     varchar(256) default 'user'            not null comment 'role user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete     tinyint      default 0                 not null comment 'is delete'
) comment 'user' collate = utf8mb4_unicode_ci;

-- question bank
create table if not exists question
(
    id          bigint auto_increment comment 'id' primary key,
    title       varchar(512)                       null comment 'title',
    content     text                               null comment 'content',
    tags        varchar(1024)                      null comment 'tags json array',
    answer      text                               null comment 'answer',
    submitNum   int      default 0                 not null comment 'submit count',
    acceptedNum int      default 0                 not null comment 'accepted count',
    judgeCase   text                               null comment 'judge cases json array',
    judgeConfig text                               null comment 'judge config json object',
    thumbNum    int      default 0                 not null comment 'thumb count',
    favourNum   int      default 0                 not null comment 'favour count',
    userId      bigint                             not null comment 'creator user id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete    tinyint  default 0                 not null comment 'is delete',
    index idx_userId (userId)
) comment 'question' collate = utf8mb4_unicode_ci;

-- question submit
create table if not exists question_submit
(
    id         bigint auto_increment comment 'id' primary key,
    language   varchar(128)                       not null comment 'language',
    code       text                               not null comment 'code',
    judgeInfo  text                               null comment 'judge info json object',
    status     int      default 0                 not null comment '0 waiting 1 running 2 judged 3 failed',
    questionId bigint                             not null comment 'question id',
    contestId  bigint                             null comment 'contest id',
    userId     bigint                             not null comment 'creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                 not null comment 'is delete',
    index idx_questionId (questionId),
    index idx_contestId (contestId),
    index idx_userId (userId)
) comment 'question submit' collate = utf8mb4_unicode_ci;

-- post
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment 'title',
    content    text                               null comment 'content',
    tags       varchar(1024)                      null comment 'tags json array',
    images     text                               null comment 'images json array',
    thumbNum   int      default 0                 not null comment 'thumb count',
    favourNum  int      default 0                 not null comment 'favour count',
    userId     bigint                             not null comment 'creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                 not null comment 'is delete',
    index idx_userId (userId)
) comment 'post' collate = utf8mb4_unicode_ci;

-- post thumb (hard delete)
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment 'post id',
    userId     bigint                             not null comment 'user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    index idx_postId (postId),
    index idx_userId (userId),
    unique key uk_post_user (postId, userId)
) comment 'post thumb' collate = utf8mb4_unicode_ci;

-- post favour (hard delete)
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment 'post id',
    userId     bigint                             not null comment 'user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    index idx_postId (postId),
    index idx_userId (userId),
    unique key uk_post_user (postId, userId)
) comment 'post favour' collate = utf8mb4_unicode_ci;

-- post comment
create table if not exists post_comment
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment 'post id',
    content    text                               not null comment 'content',
    userId     bigint                             not null comment 'user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                 not null comment 'is delete',
    index idx_postId (postId),
    index idx_userId (userId)
) comment 'post comment' collate = utf8mb4_unicode_ci;

-- question discussion
create table if not exists question_discussion
(
    id         bigint auto_increment comment 'id' primary key,
    questionId bigint                             not null comment 'question id',
    content    text                               not null comment 'discussion content',
    thumbNum   int      default 0                 not null comment 'thumb count',
    userId     bigint                             not null comment 'user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                 not null comment 'is delete',
    index idx_questionId (questionId),
    index idx_userId (userId),
    index idx_thumbNum (thumbNum)
) comment 'question discussion' collate = utf8mb4_unicode_ci;

-- question discussion thumb (hard delete)
create table if not exists question_discussion_thumb
(
    id           bigint auto_increment comment 'id' primary key,
    discussionId bigint                             not null comment 'discussion id',
    userId       bigint                             not null comment 'user id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    index idx_discussionId (discussionId),
    index idx_userId (userId),
    unique key uk_discussion_user (discussionId, userId)
) comment 'question discussion thumb' collate = utf8mb4_unicode_ci;

-- contest
create table if not exists contest
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(256)                       not null comment 'contest title',
    description text                              null comment 'contest description',
    startTime  datetime                           not null comment 'start time',
    endTime    datetime                           not null comment 'end time',
    inviteCode varchar(64)                        null comment 'contest invite code',
    userId     bigint                             not null comment 'creator user id',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                 not null comment 'is delete',
    index idx_userId (userId),
    index idx_time (startTime, endTime)
) comment 'contest' collate = utf8mb4_unicode_ci;

-- contest question mapping
create table if not exists contest_question
(
    id         bigint auto_increment comment 'id' primary key,
    contestId  bigint                              not null comment 'contest id',
    questionId bigint                              not null comment 'question id',
    sortOrder  int      default 0                  not null comment 'question order',
    createTime datetime default CURRENT_TIMESTAMP  not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint  default 0                  not null comment 'is delete',
    index idx_contestId (contestId),
    index idx_questionId (questionId),
    unique key uk_contest_question (contestId, questionId)
) comment 'contest question mapping' collate = utf8mb4_unicode_ci;

-- contest participant mapping
create table if not exists contest_user
(
    id         bigint auto_increment comment 'id' primary key,
    contestId  bigint                              not null comment 'contest id',
    userId     bigint                              not null comment 'user id',
    createTime datetime default CURRENT_TIMESTAMP  not null comment 'create time',
    updateTime datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment 'update time',
    index idx_contestId (contestId),
    index idx_userId (userId),
    unique key uk_contest_user (contestId, userId)
) comment 'contest participant mapping' collate = utf8mb4_unicode_ci;

-- home page event
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
