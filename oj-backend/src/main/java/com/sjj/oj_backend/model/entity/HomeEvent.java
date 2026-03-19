package com.sjj.oj_backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Home page event entity.
 */
@TableName(value = "home_event")
@Data
public class HomeEvent implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Event type, for example ACM / 天梯赛 / 蓝桥杯
     */
    private String contestType;

    /**
     * Event title
     */
    private String title;

    /**
     * Event time
     */
    private Date eventTime;

    /**
     * Event location
     */
    private String location;

    /**
     * Optional external url
     */
    private String eventUrl;

    /**
     * Description
     */
    private String description;

    /**
     * Sorting priority (smaller first)
     */
    private Integer sortOrder;

    /**
     * Creator id
     */
    private Long userId;

    /**
     * Create time
     */
    private Date createTime;

    /**
     * Update time
     */
    private Date updateTime;

    /**
     * Logical delete
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
