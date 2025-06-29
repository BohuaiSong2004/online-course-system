package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.dto.ChangePasswordRequest;
import com.bohuai.ocs.dto.LoginRequest;
import com.bohuai.ocs.dto.RegisterRequest;
import com.bohuai.ocs.dto.UserView;
import com.bohuai.ocs.mapper.UserMapper;
import com.bohuai.ocs.model.User;
import com.bohuai.ocs.security.CurrentUser;
import com.bohuai.ocs.security.Role;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    // Used to keep login timing similar whether or not the email exists.
    private static final String DUMMY_HASH = "$2a$10$d5ajOXklvhtUKYqEtj0JC.pZSGsQdB5uQFFo/QpcVstDgMWxqIqBu";

    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UserMapper users, PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional
    public TokenService.TokenPair register(RegisterRequest req) {
        if (tooLongForBcrypt(req.password())) {
            throw ApiException.badRequest("Password must be at most 72 bytes");
        }
        String email = normalize(req.email());
        if (users.findByEmail(email) != null) {
            throw ApiException.conflict("An account with that email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(req.displayName().trim());
        u.setRole(Role.LEARNER.name()); // self-registration never grants elevated roles
        try {
            users.insert(u);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("An account with that email already exists");
        }
        return tokens.issue(users.findById(u.getId()));
    }

    public TokenService.TokenPair login(LoginRequest req) {
        if (tooLongForBcrypt(req.password())) {
            throw ApiException.unauthorized("Invalid email or password"); // could never have been registered
        }
        User u = users.findByEmail(normalize(req.email()));
        String hash = u == null ? DUMMY_HASH : u.getPasswordHash();
        boolean ok = encoder.matches(req.password(), hash);
        if (u == null || !ok) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return tokens.issue(u);
    }

    public TokenService.TokenPair refresh(String rawRefreshToken) {
        return tokens.rotate(rawRefreshToken);
    }

    public void logout(String rawRefreshToken) {
        tokens.revoke(rawRefreshToken);
    }

    public UserView me(CurrentUser current) {
        return UserView.of(requireUser(current.id()));
    }

    /** Changing the password signs out every other session and returns a fresh one for this device. */
    @Transactional
    public TokenService.TokenPair changePassword(CurrentUser current, ChangePasswordRequest req) {
        User u = requireUser(current.id());
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (tooLongForBcrypt(req.newPassword())) {
            throw ApiException.badRequest("Password must be at most 72 bytes");
        }
        users.updatePassword(u.getId(), encoder.encode(req.newPassword()));
        tokens.revokeAll(u.getId());
        return tokens.issue(u);
    }

    public UserView updateProfile(CurrentUser current, String displayName) {
        users.updateDisplayName(current.id(), displayName.trim());
        return UserView.of(requireUser(current.id()));
    }

    private User requireUser(long id) {
        User u = users.findById(id);
        if (u == null) {
            throw ApiException.unauthorized("Account no longer exists");
        }
        return u;
    }

    /**
     * BCrypt only uses the first 72 bytes and Spring Security 6.5+ rejects longer input with an exception.
     * {@code @Size} counts characters, so multi-byte passwords need a byte check.
     */
    static boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > 72;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
