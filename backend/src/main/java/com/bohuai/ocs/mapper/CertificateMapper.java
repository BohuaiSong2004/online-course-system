package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.Certificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CertificateMapper {

    /** Idempotent: does nothing if the learner already holds a certificate for the course. */
    int issue(@Param("userId") long userId, @Param("courseId") long courseId, @Param("code") String code);

    Certificate findByCode(@Param("code") String code);

    Certificate findForUserCourse(@Param("userId") long userId, @Param("courseId") long courseId);

    List<Certificate> listForUser(@Param("userId") long userId);
}
