package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFilterTest {

    private final JwtService jwt = new JwtService(new OcsProperties(
            new OcsProperties.Jwt("test-secret-test-secret-test-secret-123", 3600, "ocs_token", false),
            new OcsProperties.RateLimit(false, 1, 1),
            new OcsProperties.Cors(List.of("http://localhost:3000"))), new ObjectMapper());
    private final AuthFilter filter = new AuthFilter(jwt, "ocs_token", List.of("http://localhost:3000"));
    private final String token = jwt.issue(new CurrentUser(7, "Leo", Role.LEARNER));

    private Object run(String method, String origin, boolean bearer) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/api/courses/1/enroll");
        if (origin != null) {
            req.addHeader("Origin", origin);
        }
        if (bearer) {
            req.addHeader("Authorization", "Bearer " + token);
        } else {
            req.setCookies(new Cookie("ocs_token", token));
        }
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return req.getAttribute(AuthFilter.ATTR);
    }

    @Test
    void cookieWorksForReadsFromAnyOrigin() throws Exception {
        assertThat(run("GET", "https://evil.example", false)).isNotNull();
    }

    @Test
    void cookieWorksForWritesFromAllowedOriginOrNoOrigin() throws Exception {
        assertThat(run("POST", "http://localhost:3000", false)).isNotNull();
        assertThat(run("POST", null, false)).isNotNull();
    }

    @Test
    void cookieIgnoredForCrossSiteWrites() throws Exception {
        assertThat(run("POST", "https://evil.example", false)).isNull();
        assertThat(run("DELETE", "https://evil.example", false)).isNull();
    }

    @Test
    void bearerIsNotSubjectToOriginCheck() throws Exception {
        assertThat(run("POST", "https://evil.example", true)).isNotNull();
    }
}
