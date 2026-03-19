package com.sjj.oj_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjj.oj_backend.model.dto.contest.ContestAddRequest;
import com.sjj.oj_backend.model.dto.contest.ContestQueryRequest;
import com.sjj.oj_backend.model.dto.contest.ContestUpdateRequest;
import com.sjj.oj_backend.model.entity.Contest;
import com.sjj.oj_backend.model.entity.User;
import com.sjj.oj_backend.model.vo.ContestRankVO;
import com.sjj.oj_backend.model.vo.ContestVO;

import java.util.List;

/**
 * Contest service
 */
public interface ContestService extends IService<Contest> {

    /**
     * Validate contest data
     *
     * @param contest contest object
     * @param add     is create mode
     */
    void validContest(Contest contest, boolean add);

    /**
     * Build query conditions
     *
     * @param contestQueryRequest query request
     * @return wrapper
     */
    QueryWrapper<Contest> getQueryWrapper(ContestQueryRequest contestQueryRequest);

    /**
     * Create contest
     *
     * @param contestAddRequest request
     * @param loginUser         creator
     * @return contest id
     */
    long addContest(ContestAddRequest contestAddRequest, User loginUser);

    /**
     * Update contest
     *
     * @param contestUpdateRequest request
     * @return result
     */
    boolean updateContest(ContestUpdateRequest contestUpdateRequest);

    /**
     * Join contest
     *
     * @param contestId contest id
     * @param inviteCode invite code
     * @param loginUser current user
     * @return result
     */
    boolean joinContest(Long contestId, String inviteCode, User loginUser);

    /**
     * Get contest detail view
     *
     * @param contest contest
     * @param loginUser login user (nullable)
     * @param withQuestions include questions
     * @param inviteCode invite code
     * @return view object
     */
    ContestVO getContestVO(Contest contest, User loginUser, boolean withQuestions, String inviteCode);

    /**
     * Convert page
     *
     * @param contestPage page
     * @param loginUser current user
     * @return view page
     */
    Page<ContestVO> getContestVOPage(Page<Contest> contestPage, User loginUser);

    /**
     * Real-time rank list
     *
     * @param contestId contest id
     * @return ranking
     */
    List<ContestRankVO> getContestRankList(Long contestId);

    /**
     * Get contest status
     *
     * @param contest contest
     * @return not_started / running / ended
     */
    String getContestStatus(Contest contest);
}


