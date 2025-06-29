package com.bohuai.ocs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the caller from either an {@code Authorization: Bearer} header (API clients, SSR)
 * or the httpOnly access-token cookie (browser). Never rejects: endpoints decide via {@link Auth}.
 * Cookies are ignored on cross-site state-changing requests (see {@link OriginPolicy}).
 */
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR = "ocs.currentUser";

    private final JwtService jwt;
    private final String cookieName;
    private final OriginPolicy originPolicy;

    public AuthFilter(JwtService jwt, String cookieName, OriginPolicy originPolicy) {
        this.jwt = jwt;
        this.cookieName = cookieName;
        this.originPolicy = originPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        } else if (originPolicy.cookiesAllowed(request)) {
            token = readCookie(request, cookieName);
        }
        jwt.verify(token).ifPresent(user -> request.setAttribute(ATTR, user));
        chain.doFilter(request, response);
    }

    public static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
