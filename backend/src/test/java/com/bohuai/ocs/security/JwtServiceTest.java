package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static OcsProperties props(String secret, long ttl) {
        return new OcsProperties(
                new OcsProperties.Jwt(secret, ttl, 3600, 10, "ocs_token", "ocs_refresh", false),
                new OcsProperties.RateLimit(false, 1, 1),
                new OcsProperties.Cors(List.of("http://localhost:3000")));
    }

    private final JwtService jwt = new JwtService(props("test-secret-test-secret-test-secret-123", 900));

    @Test
    void roundTripsClaims() {
        String token = jwt.issue(new CurrentUser(42, "Leo", Role.INSTRUCTOR));
        assertThat(jwt.verify(token)).contains(new CurrentUser(42, "Leo", Role.INSTRUCTOR));
    }

    @Test
    void rejectsTamperedPayload() {
        String token = jwt.issue(new CurrentUser(1, "Leo", Role.LEARNER));
        String[] p = token.split("\\.");
        String forged = p[0] + "." + p[1].substring(0, p[1].length() - 2) + "xx." + p[2];
        assertThat(jwt.verify(forged)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(props("another-secret-another-secret-another-1", 900));
        assertThat(jwt.verify(other.issue(new CurrentUser(1, "x", Role.ADMIN)))).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        String old = jwt.issue(new CurrentUser(1, "x", Role.LEARNER), Instant.now().minusSeconds(3600));
        assertThat(jwt.verify(old)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwt.verify(null)).isEmpty();
        assertThat(jwt.verify("not-a-jwt")).isEmpty();
        assertThat(jwt.verify("a.b.c")).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("short", 60))).isInstanceOf(IllegalStateException.class);
    }
}
