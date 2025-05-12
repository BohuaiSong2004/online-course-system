package com.bohuai.ocs.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    @Test
    void detectsPasswordsOverBcryptByteLimit() {
        assertThat(AuthService.tooLongForBcrypt("a".repeat(72))).isFalse();
        assertThat(AuthService.tooLongForBcrypt("a".repeat(73))).isTrue();
        // 30 CJK characters = 90 UTF-8 bytes: passes @Size(max = 72) but exceeds BCrypt's limit
        assertThat(AuthService.tooLongForBcrypt("密".repeat(30))).isTrue();
    }
}
