package com.bohuai.ocs.dto;

import com.bohuai.ocs.service.TokenService;

/** Tokens are also set as httpOnly cookies; the body copy is for non-browser API clients. */
public record AuthResponse(String token, long expiresIn, String refreshToken, long refreshExpiresIn, UserView user) {

    public static AuthResponse of(TokenService.TokenPair p) {
        return new AuthResponse(p.accessToken(), p.accessTtlSeconds(), p.refreshToken(), p.refreshTtlSeconds(),
                UserView.of(p.user()));
    }
}
