package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper {

    int insert(@Param("userId") long userId, @Param("courseId") Long courseId,
               @Param("chapterId") Long chapterId, @Param("action") String action);

    /** Served by idx_activity_user_created: index range scan, no filesort. */
    List<Activity> recentForUser(@Param("userId") long userId, @Param("limit") int limit);
}
