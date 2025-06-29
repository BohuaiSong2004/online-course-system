package com.bohuai.ocs.controller;

import com.bohuai.ocs.dto.AuthResponse;
import com.bohuai.ocs.dto.ChangePasswordRequest;
import com.bohuai.ocs.dto.ProfileRequest;
import com.bohuai.ocs.dto.UserView;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.security.SessionCookies;
import com.bohuai.ocs.service.AuthService;
import com.bohuai.ocs.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service account settings for the signed-in user. */
@RestController
@RequestMapping("/api/me")
public class AccountController {

    private final Auth auth;
    private final AuthService authService;
    private final SessionCookies cookies;

    public AccountController(Auth auth, AuthService authService, SessionCookies cookies) {
        this.auth = auth;
        this.authService = authService;
        this.cookies = cookies;
    }

    @PutMapping("/profile")
    public UserView updateProfile(@Valid @RequestBody ProfileRequest req) {
        return authService.updateProfile(auth.require(), req.displayName());
    }

    /** Signs out all other sessions; this device receives a fresh session. */
    @PutMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        TokenService.TokenPair pair = authService.changePassword(auth.require(), req);
        return ResponseEntity.ok().headers(cookies.set(pair)).body(AuthResponse.of(pair));
    }
}
