package com.bohuai.ocs.controller;

import com.bohuai.ocs.config.OcsProperties;
import com.bohuai.ocs.dto.AuthResponse;
import com.bohuai.ocs.dto.LoginRequest;
import com.bohuai.ocs.dto.RegisterRequest;
import com.bohuai.ocs.dto.UserView;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Stateless auth. The token is returned in the body (for API clients) and as an httpOnly,
 * SameSite=Lax cookie (for the browser, so page JavaScript never touches it).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Auth auth;
    private final OcsProperties.Jwt jwtProps;

    public AuthController(AuthService authService, Auth auth, OcsProperties props) {
        this.authService = authService;
        this.auth = auth;
        this.jwtProps = props.jwt();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return withCookie(ResponseEntity.status(HttpStatus.CREATED), res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return withCookie(ResponseEntity.ok(), authService.login(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cleared = cookie("", Duration.ZERO);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    @GetMapping("/me")
    public UserView me() {
        return authService.me(auth.require());
    }

    private ResponseEntity<AuthResponse> withCookie(ResponseEntity.BodyBuilder builder, AuthResponse res) {
        ResponseCookie c = cookie(res.token(), Duration.ofSeconds(res.expiresIn()));
        return builder.header(HttpHeaders.SET_COOKIE, c.toString()).body(res);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(jwtProps.cookieName(), value)
                .httpOnly(true)
                .secure(jwtProps.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
