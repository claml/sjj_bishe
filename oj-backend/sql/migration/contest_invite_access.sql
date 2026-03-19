alter table contest
    add column inviteCode varchar(64) null comment 'contest invite code' after endTime;
