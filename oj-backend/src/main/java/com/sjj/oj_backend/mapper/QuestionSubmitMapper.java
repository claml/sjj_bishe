package com.sjj.oj_backend.mapper;

import com.sjj.oj_backend.model.entity.QuestionSubmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sjj.oj_backend.model.vo.AcceptedQuestionSubmitVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 石家炅
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2026-01-06 11:32:37
* @Entity com.sjj.oj_backend.model.entity.QuestionSubmit
*/
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    /**
     * Count distinct accepted questions of a user.
     */
    @Select("SELECT COUNT(DISTINCT questionId) " +
            "FROM question_submit " +
            "WHERE userId = #{userId} " +
            "AND status = 2 " +
            "AND isDelete = 0")
    long countDistinctAcceptedQuestionByUserId(@Param("userId") Long userId);

    /**
     * List latest accepted submission per question.
     */
    @Select("SELECT qs.id AS submitId, qs.questionId AS questionId, q.title AS questionTitle, " +
            "(CASE WHEN q.id IS NULL THEN NULL ELSE (" +
            "  SELECT COUNT(1) " +
            "  FROM question q2 " +
            "  WHERE q2.isDelete = 0 " +
            "    AND (q2.createTime > q.createTime OR (q2.createTime = q.createTime AND q2.id >= q.id))" +
            ") END) AS questionNo, " +
            "qs.language AS language, qs.code AS code, qs.createTime AS submitTime " +
            "FROM question_submit qs " +
            "JOIN ( " +
            "  SELECT questionId, MAX(id) AS maxId " +
            "  FROM question_submit " +
            "  WHERE userId = #{userId} " +
            "    AND status = 2 " +
            "    AND isDelete = 0 " +
            "  GROUP BY questionId " +
            ") latest ON latest.maxId = qs.id " +
            "LEFT JOIN question q ON q.id = qs.questionId AND q.isDelete = 0 " +
            "ORDER BY qs.id DESC " +
            "LIMIT #{offset}, #{size}")
    List<AcceptedQuestionSubmitVO> listLatestAcceptedQuestionByUserId(@Param("userId") Long userId,
                                                                      @Param("offset") long offset,
                                                                      @Param("size") long size);
}




