-- add contestId for submit table
alter table question_submit
    add column contestId bigint null comment 'contest id' after questionId;

create index idx_contestId on question_submit (contestId);

-- contest table
create table if not exists contest
(
    id          bigint auto_increment comment 'id' primary key,
    title       varchar(256)                       not null comment 'contest title',
    description text                               null comment 'contest description',
    startTime   datetime                           not null comment 'start time',
    endTime     datetime                           not null comment 'end time',
    inviteCode  varchar(64)                        null comment 'contest invite code',
    userId      bigint                             not null comment 'creator user id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete    tinyint  default 0                 not null comment 'is delete',
    index idx_userId (userId),
    index idx_time (startTime, endTime)
) comment 'contest' collate = utf8mb4_unicode_ci;

-- contest question relation
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

-- contest participant
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
