package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterMapper {

    /** Outline only (content omitted) ordered by position. */
    List<Chapter> findOutlineByCourse(@Param("courseId") long courseId);

    Chapter findById(@Param("id") long id);

    int nextPosition(@Param("courseId") long courseId);

    int insert(Chapter chapter);

    int update(Chapter chapter);

    int updatePosition(@Param("id") long id, @Param("courseId") long courseId, @Param("position") int position);

    int delete(@Param("id") long id);
}
