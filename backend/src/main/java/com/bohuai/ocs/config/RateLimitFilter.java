package com.bohuai.ocs.config;

import com.bohuai.ocs.ratelimit.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Per-client rate limiting with a token bucket (local or Redis-backed, see {@link RateLimiter}).
 * Each client (keyed by its remote address) may burst up to {@code capacity} requests and then
 * {@code refillPerSecond} per second; beyond that it gets HTTP 429 with a Retry-After header.
 * Mirrors the API Gateway usage plan (burst = capacity, rate = refillPerSecond) in infra/terraform.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final OcsProperties.RateLimit config;
    private final ObjectMapper mapper;
    private final RateLimiter limiter;
    private final Counter rejected;

    public RateLimitFilter(OcsProperties.RateLimit config, ObjectMapper mapper, RateLimiter limiter, MeterRegistry meters) {
        this.config = config;
        this.mapper = mapper;
        this.limiter = limiter;
        this.rejected = Counter.builder("ocs.ratelimit.rejected")
                .description("Requests refused with HTTP 429").register(meters);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !config.enabled() || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateLimiter.Decision d = limiter.tryConsume(clientKey(request));
        response.setHeader("X-RateLimit-Limit", String.valueOf(config.capacity()));
        if (d.remaining() >= 0) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(d.remaining()));
        }
        if (!d.allowed()) {
            rejected.increment();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (d.retryAfterMillis() + 999) / 1000)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(),
                    Map.of("error", "too_many_requests", "message", "Rate limit exceeded, slow down."));
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Keyed on the remote address only. X-Forwarded-For is client-controlled and would let anyone
     * bypass the limit by rotating the header; behind a trusted proxy, set
     * server.forward-headers-strategy=framework so Spring resolves the real client address first.
     */
    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
