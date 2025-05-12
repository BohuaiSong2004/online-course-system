package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static JwtService service(String secret, long ttl) {
        OcsProperties props = new OcsProperties(
                new OcsProperties.Jwt(secret, ttl, "ocs_token", false),
                new OcsProperties.RateLimit(false, 1, 1),
                new OcsProperties.Cors(List.of()));
        return new JwtService(props, new ObjectMapper());
    }

    private final JwtService jwt = service("test-secret-test-secret-test-secret-123", 3600);

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
        JwtService other = service("another-secret-another-secret-another-1", 3600);
        assertThat(jwt.verify(other.issue(new CurrentUser(1, "x", Role.ADMIN)))).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = service("test-secret-test-secret-test-secret-123", 0);
        assertThat(shortLived.verify(shortLived.issue(new CurrentUser(1, "x", Role.LEARNER)))).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwt.verify(null)).isEmpty();
        assertThat(jwt.verify("not-a-jwt")).isEmpty();
        assertThat(jwt.verify("a.b.c")).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> service("short", 60)).isInstanceOf(IllegalStateException.class);
    }
}
