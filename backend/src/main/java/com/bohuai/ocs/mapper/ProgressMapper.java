package com.bohuai.ocs.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProgressMapper {

    /** Idempotent: returns 1 when newly completed, 0 if it already was. */
    int markComplete(@Param("userId") long userId, @Param("chapterId") long chapterId,
                     @Param("courseId") long courseId);

    List<Long> completedChapterIds(@Param("userId") long userId, @Param("courseId") long courseId);

    int deleteForCourse(@Param("userId") long userId, @Param("courseId") long courseId);
}
