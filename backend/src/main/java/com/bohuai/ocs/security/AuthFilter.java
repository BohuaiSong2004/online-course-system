package com.bohuai.ocs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Resolves the caller from either an {@code Authorization: Bearer} header (API clients, SSR)
 * or the httpOnly session cookie (browser). Never rejects: endpoints decide via {@link Auth}.
 * <p>
 * CSRF defence in depth (on top of SameSite=Lax): for state-changing requests, the cookie is only
 * honoured when the {@code Origin} header is absent or is one of the allowed front-end origins.
 * Bearer tokens are not ambient credentials, so they need no such check.
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR = "ocs.currentUser";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final JwtService jwt;
    private final String cookieName;
    private final Set<String> allowedOrigins;

    public AuthFilter(JwtService jwt, String cookieName, List<String> allowedOrigins) {
        this.jwt = jwt;
        this.cookieName = cookieName;
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        } else if (cookieAllowed(request)) {
            token = readCookie(request);
        }
        jwt.verify(token).ifPresent(user -> request.setAttribute(ATTR, user));
        chain.doFilter(request, response);
    }

    private boolean cookieAllowed(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }
        String origin = request.getHeader("Origin");
        return origin == null || allowedOrigins.contains(origin);
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (cookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
