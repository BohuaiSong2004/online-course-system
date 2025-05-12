package com.bohuai.ocs.dto;

public record AuthResponse(String token, long expiresIn, UserView user) {
}
