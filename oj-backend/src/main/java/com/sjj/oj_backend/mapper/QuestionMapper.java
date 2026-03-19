package com.sjj.oj_backend.mapper;

import com.sjj.oj_backend.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author 石家炅
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2026-01-06 11:31:23
* @Entity com.sjj.oj_backend.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * Increase submit count by question id.
     */
    @Update("UPDATE question " +
            "SET submitNum = IFNULL(submitNum, 0) + 1 " +
            "WHERE id = #{questionId} AND isDelete = 0")
    int increaseSubmitNum(@Param("questionId") Long questionId);

    /**
     * Increase accepted count by question id.
     */
    @Update("UPDATE question " +
            "SET acceptedNum = IFNULL(acceptedNum, 0) + 1 " +
            "WHERE id = #{questionId} AND isDelete = 0")
    int increaseAcceptedNum(@Param("questionId") Long questionId);
}




