package com.bohuai.ocs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client token-bucket rate limiter.
 * <p>
 * Each client (keyed by its remote address) gets a bucket
 * of {@code capacity} tokens that refills at {@code refillPerSecond}. A request costs one token;
 * an empty bucket yields HTTP 429 with a Retry-After header. Mirrors the API Gateway usage plan
 * (burst = capacity, rate = refillPerSecond) defined in infra/terraform.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final OcsProperties.RateLimit config;
    private final ObjectMapper mapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(OcsProperties.RateLimit config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !config.enabled() || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            evictIdle();
        }
        Bucket bucket = buckets.computeIfAbsent(clientKey(request),
                k -> new Bucket(config.capacity(), config.refillPerSecond()));

        long waitMillis = bucket.tryConsume();
        response.setHeader("X-RateLimit-Limit", String.valueOf(config.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.remaining()));
        if (waitMillis > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (waitMillis + 999) / 1000)));
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
    /**
     * Memory guard: drops buckets that have refilled completely (clients that went quiet). Clearing
     * everything instead would hand a fresh burst to clients that are currently being throttled.
     */
    void evictIdle() {
        buckets.values().removeIf(Bucket::isFull);
    }

    int trackedClients() {
        return buckets.size();
    }

    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    static final class Bucket {
        private final int capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefill;

        Bucket(int capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000d;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        /** @return 0 if a token was consumed, otherwise milliseconds until one is available. */
        synchronized long tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            double missing = 1 - tokens;
            return (long) Math.ceil(missing / refillPerNano / 1_000_000d);
        }

        synchronized boolean isFull() {
            refill();
            return tokens >= capacity;
        }

        synchronized int remaining() {
            return (int) Math.floor(tokens);
        }

        private void refill() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
            lastRefill = now;
        }
    }
}
