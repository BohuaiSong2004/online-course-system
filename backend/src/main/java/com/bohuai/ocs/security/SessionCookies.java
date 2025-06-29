package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import com.bohuai.ocs.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Builds the httpOnly, SameSite=Lax session cookies (access + refresh). JavaScript never sees them. */
@Component
public class SessionCookies {

    private final OcsProperties.Jwt cfg;

    public SessionCookies(OcsProperties props) {
        this.cfg = props.jwt();
    }

    public HttpHeaders set(TokenService.TokenPair pair) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.SET_COOKIE, cookie(cfg.cookieName(), pair.accessToken(),
                Duration.ofSeconds(pair.accessTtlSeconds())).toString());
        h.add(HttpHeaders.SET_COOKIE, cookie(cfg.refreshCookieName(), pair.refreshToken(),
                Duration.ofSeconds(pair.refreshTtlSeconds())).toString());
        return h;
    }

    public HttpHeaders clear() {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.SET_COOKIE, cookie(cfg.cookieName(), "", Duration.ZERO).toString());
        h.add(HttpHeaders.SET_COOKIE, cookie(cfg.refreshCookieName(), "", Duration.ZERO).toString());
        return h;
    }

    public String refreshToken(HttpServletRequest request) {
        return AuthFilter.readCookie(request, cfg.refreshCookieName());
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cfg.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
