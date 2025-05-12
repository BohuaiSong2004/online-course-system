package com.bohuai.ocs.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitBucketTest {

    @Test
    void allowsBurstThenThrottles() {
        RateLimitFilter.Bucket bucket = new RateLimitFilter.Bucket(3, 1.0);
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        long wait = bucket.tryConsume();
        assertThat(wait).isPositive().isLessThanOrEqualTo(1000);
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        RateLimitFilter.Bucket bucket = new RateLimitFilter.Bucket(1, 20.0); // one token every 50ms
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isPositive();
        Thread.sleep(120);
        assertThat(bucket.tryConsume()).isZero();
    }

    @Test
    void evictionKeepsThrottledClients() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new OcsProperties.RateLimit(true, 1, 0.001),
                new com.fasterxml.jackson.databind.ObjectMapper());
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/x");
        req.setRemoteAddr("10.0.0.1");
        org.springframework.mock.web.MockHttpServletResponse first = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(req, first, new org.springframework.mock.web.MockFilterChain());
        org.springframework.mock.web.MockHttpServletResponse second = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(req, second, new org.springframework.mock.web.MockFilterChain());
        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);

        filter.evictIdle(); // the throttled client's bucket is empty, so it must survive eviction
        assertThat(filter.trackedClients()).isEqualTo(1);
    }

    @Test
    void ignoresSpoofedForwardedFor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new OcsProperties.RateLimit(true, 1, 0.001),
                new com.fasterxml.jackson.databind.ObjectMapper());
        int throttled = 0;
        for (int i = 0; i < 3; i++) {
            org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/x");
            req.setRemoteAddr("10.0.0.2");
            req.addHeader("X-Forwarded-For", "1.2.3." + i); // rotating the header must not reset the bucket
            org.springframework.mock.web.MockHttpServletResponse res = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(req, res, new org.springframework.mock.web.MockFilterChain());
            if (res.getStatus() == 429) {
                throttled++;
            }
        }
        assertThat(throttled).isEqualTo(2);
    }
}
