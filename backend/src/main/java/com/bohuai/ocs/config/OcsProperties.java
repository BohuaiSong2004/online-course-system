package com.bohuai.ocs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Application settings, bound from the {@code ocs.*} namespace in application.yml.
 */
@ConfigurationProperties(prefix = "ocs")
public record OcsProperties(Jwt jwt, RateLimit rateLimit, Cors cors) {

    /**
     * @param accessTtlSeconds  lifetime of the stateless access token (short, so role changes and
     *                          revocations take effect quickly)
     * @param refreshTtlSeconds lifetime of a rotating refresh token stored server-side
     * @param reuseGraceSeconds window after rotation in which the old refresh token is still accepted
     *                          (concurrent requests), before reuse is treated as theft
     */
    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds, long reuseGraceSeconds,
                      String cookieName, String refreshCookieName, boolean cookieSecure) {
    }

    /**
     * In-process token bucket. In AWS the same limits are enforced at API Gateway
     * (see infra/terraform); this keeps local/dev behaviour identical.
     */
    public record RateLimit(boolean enabled, int capacity, double refillPerSecond) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
