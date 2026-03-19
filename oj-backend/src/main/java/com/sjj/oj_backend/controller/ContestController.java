package com.sjj.oj_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjj.oj_backend.annotation.AuthCheck;
import com.sjj.oj_backend.common.BaseResponse;
import com.sjj.oj_backend.common.DeleteRequest;
import com.sjj.oj_backend.common.ErrorCode;
import com.sjj.oj_backend.common.ResultUtils;
import com.sjj.oj_backend.constant.UserConstant;
import com.sjj.oj_backend.exception.BusinessException;
import com.sjj.oj_backend.exception.ThrowUtils;
import com.sjj.oj_backend.model.dto.contest.ContestAddRequest;
import com.sjj.oj_backend.model.dto.contest.ContestJoinRequest;
import com.sjj.oj_backend.model.dto.contest.ContestQueryRequest;
import com.sjj.oj_backend.model.dto.contest.ContestUpdateRequest;
import com.sjj.oj_backend.model.entity.Contest;
import com.sjj.oj_backend.model.entity.ContestQuestion;
import com.sjj.oj_backend.model.entity.ContestUser;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.ContestRankVO;
import com.sjj.oj_backend.model.vo.ContestVO;
import com.sjj.oj_backend.service.ContestQuestionService;
import com.sjj.oj_backend.service.ContestService;
import com.sjj.oj_backend.service.ContestUserService;
import com.sjj.oj_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Contest controller
 */
@RestController
@RequestMapping("/contest")
@Slf4j
public class ContestController {

    @Resource
    private ContestService contestService;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    private ContestUserService contestUserService;

    @Resource
    private UserService userService;

    /**
     * Create contest (admin)
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addContest(@RequestBody ContestAddRequest contestAddRequest, HttpServletRequest request) {
        if (contestAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long contestId = contestService.addContest(contestAddRequest, loginUser);
        return ResultUtils.success(contestId);
    }

    /**
     * Delete contest (admin)
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteContest(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long contestId = deleteRequest.getId();
        Contest oldContest = contestService.getById(contestId);
        ThrowUtils.throwIf(oldContest == null, ErrorCode.NOT_FOUND_ERROR, "Contest not found");

        boolean removeContest = contestService.removeById(contestId);
        ThrowUtils.throwIf(!removeContest, ErrorCode.OPERATION_ERROR, "Delete contest failed");

        contestQuestionService.remove(new QueryWrapper<ContestQuestion>().eq("contestId", contestId));
        contestUserService.remove(new QueryWrapper<ContestUser>().eq("contestId", contestId));
        return ResultUtils.success(true);
    }

    /**
     * Update contest basic info (admin)
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateContest(@RequestBody ContestUpdateRequest contestUpdateRequest) {
        if (contestUpdateRequest == null || contestUpdateRequest.getId() == null || contestUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = contestService.updateContest(contestUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * List contests for users
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ContestVO>> listContestVOByPage(@RequestBody ContestQueryRequest contestQueryRequest,
                                                              HttpServletRequest request) {
        if (contestQueryRequest == null) {
            contestQueryRequest = new ContestQueryRequest();
        }
        long current = contestQueryRequest.getCurrent();
        long size = contestQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<Contest> contestPage = contestService.page(new Page<>(current, size),
                contestService.getQueryWrapper(contestQueryRequest));
        User loginUser = userService.getLoginUserPermitNull(request);
        return ResultUtils.success(contestService.getContestVOPage(contestPage, loginUser));
    }

    /**
     * List contests for admin
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Contest>> listContestByPage(@RequestBody ContestQueryRequest contestQueryRequest) {
        if (contestQueryRequest == null) {
            contestQueryRequest = new ContestQueryRequest();
        }
        long current = contestQueryRequest.getCurrent();
        long size = contestQueryRequest.getPageSize();
        Page<Contest> contestPage = contestService.page(new Page<>(current, size),
                contestService.getQueryWrapper(contestQueryRequest));
        return ResultUtils.success(contestPage);
    }

    /**
     * Contest detail
     */
    @GetMapping("/get/vo")
    public BaseResponse<ContestVO> getContestVOById(long id, String inviteCode, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Contest contest = contestService.getById(id);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "Contest not found");
        User loginUser = userService.getLoginUserPermitNull(request);
        ContestVO contestVO = contestService.getContestVO(contest, loginUser, true, inviteCode);
        return ResultUtils.success(contestVO);
    }

    /**
     * Join contest
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinContest(@RequestBody ContestJoinRequest contestJoinRequest,
                                             HttpServletRequest request) {
        if (contestJoinRequest == null || contestJoinRequest.getContestId() == null
                || contestJoinRequest.getContestId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = contestService.joinContest(
                contestJoinRequest.getContestId(),
                contestJoinRequest.getInviteCode(),
                loginUser
        );
        return ResultUtils.success(result);
    }

    /**
     * Real-time rank list
     */
    @GetMapping("/rank")
    public BaseResponse<List<ContestRankVO>> getContestRank(long contestId) {
        if (contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(contestService.getContestRankList(contestId));
    }
}

