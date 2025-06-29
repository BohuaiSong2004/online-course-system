package com.bohuai.ocs.dto;

/** Optional body for /api/auth/refresh and /logout; browsers send the refresh cookie instead. */
public record RefreshRequest(String refreshToken) {
}
