package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.QuizAttempt;
import com.bohuai.ocs.model.QuizOption;
import com.bohuai.ocs.model.QuizQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizMapper {

    /** Questions with their options (including the correct flags), ordered. */
    List<QuizQuestion> findQuestions(@Param("chapterId") long chapterId);

    int deleteQuestions(@Param("chapterId") long chapterId);

    int insertQuestion(QuizQuestion question);

    int insertOption(QuizOption option);

    int updatePassPercent(@Param("chapterId") long chapterId, @Param("passPercent") int passPercent);

    int insertAttempt(QuizAttempt attempt);

    /** The caller's best attempt (highest score, then latest), or null. */
    QuizAttempt bestAttempt(@Param("userId") long userId, @Param("chapterId") long chapterId);
}
