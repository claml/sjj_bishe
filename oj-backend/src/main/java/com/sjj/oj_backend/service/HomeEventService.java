package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventAddRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventQueryRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventUpdateRequest;
import com.sjj.oj_backend.model.entity.HomeEvent;
import com.sjj.oj_backend.model.entity.User;

import java.util.List;

/**
 * Home event service.
 */
public interface HomeEventService extends IService<HomeEvent> {

    /**
     * Validate home event data.
     *
     * @param homeEvent entity
     * @param add       add mode
     */
    void validHomeEvent(HomeEvent homeEvent, boolean add);

    /**
     * Build query wrapper.
     *
     * @param homeEventQueryRequest request
     * @return wrapper
     */
    QueryWrapper<HomeEvent> getQueryWrapper(HomeEventQueryRequest homeEventQueryRequest);

    /**
     * Add home event.
     *
     * @param addRequest add request
     * @param loginUser  current user
     * @return new id
     */
    long addHomeEvent(HomeEventAddRequest addRequest, User loginUser);

    /**
     * Update home event.
     *
     * @param updateRequest update request
     * @return result
     */
    boolean updateHomeEvent(HomeEventUpdateRequest updateRequest);

    /**
     * List upcoming events.
     *
     * @param limit max item count
     * @return event list
     */
    List<HomeEvent> listUpcoming(int limit);
}
