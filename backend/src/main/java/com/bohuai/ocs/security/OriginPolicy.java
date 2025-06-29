package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CSRF defence in depth (on top of SameSite=Lax cookies): state-changing requests may only use the
 * session cookies when the {@code Origin} header is absent (same-origin navigations, server-side
 * calls) or one of the configured front-end origins.
 */
@Component
public class OriginPolicy {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> allowedOrigins;

    public OriginPolicy(OcsProperties props) {
        this.allowedOrigins = Set.copyOf(props.cors().allowedOrigins());
    }

    public boolean cookiesAllowed(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }
        String origin = request.getHeader("Origin");
        return origin == null || allowedOrigins.contains(origin);
    }
}
