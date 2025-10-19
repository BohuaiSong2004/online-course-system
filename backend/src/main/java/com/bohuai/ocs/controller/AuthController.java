package com.bohuai.ocs.controller;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.dto.AuthResponse;
import com.bohuai.ocs.dto.LoginRequest;
import com.bohuai.ocs.dto.RefreshRequest;
import com.bohuai.ocs.dto.RegisterRequest;
import com.bohuai.ocs.dto.UserView;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.security.OriginPolicy;
import com.bohuai.ocs.security.SessionCookies;
import com.bohuai.ocs.service.AuthService;
import com.bohuai.ocs.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessions. Browsers get both tokens as httpOnly cookies; API clients use the body copies and send
 * {@code Authorization: Bearer} plus {"refreshToken": ...} to /refresh.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Auth auth;
    private final SessionCookies cookies;
    private final OriginPolicy originPolicy;

    public AuthController(AuthService authService, Auth auth, SessionCookies cookies, OriginPolicy originPolicy) {
        this.authService = authService;
        this.auth = auth;
        this.cookies = cookies;
        this.originPolicy = originPolicy;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return respond(HttpStatus.CREATED, authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return respond(HttpStatus.OK, authService.login(req));
    }

    /** Rotates the refresh token. A reused (already rotated) token revokes all of the user's sessions. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshRequest body,
                                                HttpServletRequest request) {
        try {
            return respond(HttpStatus.OK, authService.refresh(refreshToken(body, request)));
        } catch (ApiException e) {
            return ResponseEntity.status(e.status()).headers(cookies.clear()).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest body, HttpServletRequest request) {
        authService.logout(refreshToken(body, request));
        return ResponseEntity.noContent().headers(cookies.clear()).build();
    }

    @GetMapping("/me")
    public UserView me() {
        return authService.me(auth.require());
    }

    /** Like /me but never 401: {"user": null} for anonymous visitors (used by the page header). */
    @GetMapping("/session")
    public java.util.Map<String, UserView> session() {
        java.util.Map<String, UserView> body = new java.util.HashMap<>();
        body.put("user", auth.current().map(authService::meOrNull).orElse(null));
        return body;
    }

    private String refreshToken(RefreshRequest body, HttpServletRequest request) {
        if (body != null && body.refreshToken() != null) {
            return body.refreshToken();
        }
        return originPolicy.cookiesAllowed(request) ? cookies.refreshToken(request) : null;
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, TokenService.TokenPair pair) {
        return ResponseEntity.status(status).headers(cookies.set(pair)).body(AuthResponse.of(pair));
    }
}
