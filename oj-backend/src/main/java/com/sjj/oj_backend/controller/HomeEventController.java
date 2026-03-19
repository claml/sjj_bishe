package com.sjj.oj_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.annotation.AuthCheck;
import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.DeleteRequest;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.constant.UserConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventAddRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventQueryRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventUpdateRequest;
import com.sjj.oj_backend.model.entity.HomeEvent;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.service.HomeEventService;
import com.sjj.oj_backend.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Home event controller.
 */
@RestController
@RequestMapping("/homeEvent")
public class HomeEventController {

    @Resource
    private HomeEventService homeEventService;

    @Resource
    private UserService userService;

    /**
     * Create home event (admin).
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addHomeEvent(@RequestBody HomeEventAddRequest addRequest, HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long eventId = homeEventService.addHomeEvent(addRequest, loginUser);
        return ResultUtils.success(eventId);
    }

    /**
     * Delete home event (admin).
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteHomeEvent(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        HomeEvent oldHomeEvent = homeEventService.getById(id);
        ThrowUtils.throwIf(oldHomeEvent == null, ErrorCode.NOT_FOUND_ERROR, "Home event not found");
        boolean result = homeEventService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "Delete home event failed");
        return ResultUtils.success(true);
    }

    /**
     * Update home event (admin).
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateHomeEvent(@RequestBody HomeEventUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = homeEventService.updateHomeEvent(updateRequest);
        return ResultUtils.success(result);
    }

    /**
     * Page list for admin.
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<HomeEvent>> listHomeEventByPage(@RequestBody HomeEventQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new HomeEventQueryRequest();
        }
        long current = queryRequest.getCurrent();
        long pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 100, ErrorCode.PARAMS_ERROR);
        Page<HomeEvent> homeEventPage = homeEventService.page(new Page<>(current, pageSize),
                homeEventService.getQueryWrapper(queryRequest));
        return ResultUtils.success(homeEventPage);
    }

    /**
     * Upcoming events for home page.
     */
    @GetMapping("/list/upcoming")
    public BaseResponse<List<HomeEvent>> listUpcomingHomeEvents(Integer limit) {
        int safeLimit = ObjectUtils.defaultIfNull(limit, 20);
        return ResultUtils.success(homeEventService.listUpcoming(safeLimit));
    }
}
