package com.sjj.oj_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.constant.CommonConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.mapper.HomeEventMapper;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventAddRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventQueryRequest;
import com.sjj.oj_backend.model.dto.homeevent.HomeEventUpdateRequest;
import com.sjj.oj_backend.model.entity.HomeEvent;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.service.HomeEventService;
import com.sjj.oj_backend.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Home event service implementation.
 */
@Service
public class HomeEventServiceImpl extends ServiceImpl<HomeEventMapper, HomeEvent> implements HomeEventService {

    private static final List<String> DATE_TIME_PATTERNS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
    );

    @Override
    public void validHomeEvent(HomeEvent homeEvent, boolean add) {
        if (homeEvent == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String contestType = homeEvent.getContestType();
        String title = homeEvent.getTitle();
        Date eventTime = homeEvent.getEventTime();
        String location = homeEvent.getLocation();
        String eventUrl = homeEvent.getEventUrl();
        String description = homeEvent.getDescription();
        Integer sortOrder = homeEvent.getSortOrder();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(contestType), ErrorCode.PARAMS_ERROR, "Event type is required");
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR, "Event title is required");
            ThrowUtils.throwIf(eventTime == null, ErrorCode.PARAMS_ERROR, "Event time is required");
        }
        ThrowUtils.throwIf(StringUtils.length(contestType) > 32, ErrorCode.PARAMS_ERROR, "Event type is too long");
        ThrowUtils.throwIf(StringUtils.length(title) > 128, ErrorCode.PARAMS_ERROR, "Event title is too long");
        ThrowUtils.throwIf(StringUtils.length(location) > 128, ErrorCode.PARAMS_ERROR, "Event location is too long");
        ThrowUtils.throwIf(StringUtils.length(eventUrl) > 512, ErrorCode.PARAMS_ERROR, "Event url is too long");
        ThrowUtils.throwIf(StringUtils.length(description) > 1024, ErrorCode.PARAMS_ERROR, "Event description is too long");
        ThrowUtils.throwIf(sortOrder != null && sortOrder < 0, ErrorCode.PARAMS_ERROR, "Sort order cannot be negative");
    }

    @Override
    public QueryWrapper<HomeEvent> getQueryWrapper(HomeEventQueryRequest homeEventQueryRequest) {
        QueryWrapper<HomeEvent> queryWrapper = new QueryWrapper<>();
        if (homeEventQueryRequest == null) {
            queryWrapper.eq("isDelete", false);
            return queryWrapper;
        }
        String title = homeEventQueryRequest.getTitle();
        String contestType = homeEventQueryRequest.getContestType();
        String sortField = homeEventQueryRequest.getSortField();
        String sortOrder = homeEventQueryRequest.getSortOrder();

        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.eq(StringUtils.isNotBlank(contestType), "contestType", contestType);
        queryWrapper.eq("isDelete", false);

        if (!SqlUtils.validSortField(sortField)) {
            sortField = "eventTime";
        }
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
        queryWrapper.orderBy(true, isAsc, sortField);
        queryWrapper.orderByAsc("sortOrder", "id");
        return queryWrapper;
    }

    @Override
    public long addHomeEvent(HomeEventAddRequest addRequest, User loginUser) {
        ThrowUtils.throwIf(addRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        HomeEvent homeEvent = new HomeEvent();
        homeEvent.setContestType(StringUtils.trimToEmpty(addRequest.getContestType()));
        homeEvent.setTitle(StringUtils.trimToEmpty(addRequest.getTitle()));
        homeEvent.setEventTime(parseDateTime(addRequest.getEventTime(), "eventTime"));
        homeEvent.setLocation(StringUtils.trimToNull(addRequest.getLocation()));
        homeEvent.setEventUrl(StringUtils.trimToNull(addRequest.getEventUrl()));
        homeEvent.setDescription(StringUtils.trimToNull(addRequest.getDescription()));
        homeEvent.setSortOrder(ObjectUtils.defaultIfNull(addRequest.getSortOrder(), 0));
        homeEvent.setUserId(loginUser.getId());
        validHomeEvent(homeEvent, true);

        boolean saveResult = this.save(homeEvent);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "Create home event failed");
        return homeEvent.getId();
    }

    @Override
    public boolean updateHomeEvent(HomeEventUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        HomeEvent oldHomeEvent = this.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldHomeEvent == null, ErrorCode.NOT_FOUND_ERROR, "Home event not found");

        Date eventTime = oldHomeEvent.getEventTime();
        if (StringUtils.isNotBlank(updateRequest.getEventTime())) {
            eventTime = parseDateTime(updateRequest.getEventTime(), "eventTime");
        }
        String contestType = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(updateRequest.getContestType()), oldHomeEvent.getContestType());
        String title = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(updateRequest.getTitle()), oldHomeEvent.getTitle());
        String location = updateRequest.getLocation() == null
                ? oldHomeEvent.getLocation()
                : StringUtils.trimToNull(updateRequest.getLocation());
        String eventUrl = updateRequest.getEventUrl() == null
                ? oldHomeEvent.getEventUrl()
                : StringUtils.trimToNull(updateRequest.getEventUrl());
        String description = updateRequest.getDescription() == null
                ? oldHomeEvent.getDescription()
                : StringUtils.trimToNull(updateRequest.getDescription());
        Integer sortOrder = updateRequest.getSortOrder() == null
                ? oldHomeEvent.getSortOrder()
                : updateRequest.getSortOrder();

        HomeEvent updateHomeEvent = new HomeEvent();
        updateHomeEvent.setId(updateRequest.getId());
        updateHomeEvent.setContestType(contestType);
        updateHomeEvent.setTitle(title);
        updateHomeEvent.setEventTime(eventTime);
        updateHomeEvent.setLocation(location);
        updateHomeEvent.setEventUrl(eventUrl);
        updateHomeEvent.setDescription(description);
        updateHomeEvent.setSortOrder(sortOrder);
        validHomeEvent(updateHomeEvent, false);
        return this.updateById(updateHomeEvent);
    }

    @Override
    public List<HomeEvent> listUpcoming(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        QueryWrapper<HomeEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", false);
        queryWrapper.ge("eventTime", getTodayStart());
        queryWrapper.orderByAsc("eventTime", "sortOrder", "id");
        queryWrapper.last("limit " + safeLimit);
        return this.list(queryWrapper);
    }

    private Date parseDateTime(String dateTimeStr, String fieldName) {
        if (StringUtils.isBlank(dateTimeStr)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " is required");
        }
        for (String pattern : DATE_TIME_PATTERNS) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                simpleDateFormat.setLenient(false);
                return simpleDateFormat.parse(dateTimeStr);
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + " format is invalid");
    }

    private Date getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
