package com.bohuai.ocs.config;

import com.bohuai.ocs.ratelimit.LocalRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final RateLimitFilter filter = new RateLimitFilter(new OcsProperties.RateLimit(true, 1, 0.001),
            new ObjectMapper(), new LocalRateLimiter(1, 0.001), meters);

    private MockHttpServletResponse call(String ip, String forwardedFor) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.setRemoteAddr(ip);
        if (forwardedFor != null) {
            req.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void returns429WithRetryAfterAndCountsIt() throws Exception {
        assertThat(call("10.0.0.1", null).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call("10.0.0.1", null);
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotBlank();
        assertThat(meters.counter("ocs.ratelimit.rejected").count()).isEqualTo(1);
    }

    @Test
    void ignoresSpoofedForwardedFor() throws Exception {
        int throttled = 0;
        for (int i = 0; i < 3; i++) {
            if (call("10.0.0.2", "1.2.3." + i).getStatus() == 429) {
                throttled++;
            }
        }
        assertThat(throttled).isEqualTo(2);
    }
}
