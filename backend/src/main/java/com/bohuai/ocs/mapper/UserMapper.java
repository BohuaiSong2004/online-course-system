package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findById(@Param("id") long id);

    User findByEmail(@Param("email") String email);

    int insert(User user);

    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash);

    int updateDisplayName(@Param("id") long id, @Param("displayName") String displayName);
}
