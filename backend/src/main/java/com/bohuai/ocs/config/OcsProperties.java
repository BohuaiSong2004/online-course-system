package com.bohuai.ocs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Application settings, bound from the {@code ocs.*} namespace in application.yml.
 */
@ConfigurationProperties(prefix = "ocs")
public record OcsProperties(Jwt jwt, RateLimit rateLimit, Cors cors) {

    public record Jwt(String secret, long ttlSeconds, String cookieName, boolean cookieSecure) {
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
