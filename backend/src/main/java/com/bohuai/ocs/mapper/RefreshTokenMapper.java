package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface RefreshTokenMapper {

    int insert(RefreshToken token);

    /** Row-locks the token so two concurrent refreshes cannot both rotate it. */
    RefreshToken findByHashForUpdate(@Param("hash") String hash);

    int markReplaced(@Param("id") long id, @Param("replacedBy") long replacedBy);

    int revokeByHash(@Param("hash") String hash);

    int revokeAllForUser(@Param("userId") long userId);

    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
