package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.Enrollment;
import com.bohuai.ocs.model.EnrollmentView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentMapper {

    Enrollment find(@Param("userId") long userId, @Param("courseId") long courseId);

    int insert(Enrollment enrollment);

    int delete(@Param("userId") long userId, @Param("courseId") long courseId);

    int markCompleted(@Param("userId") long userId, @Param("courseId") long courseId);

    long countByCourse(@Param("courseId") long courseId);

    List<EnrollmentView> listForUser(@Param("userId") long userId);
}
