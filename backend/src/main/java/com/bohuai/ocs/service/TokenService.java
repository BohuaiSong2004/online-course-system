package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.config.OcsProperties;
import com.bohuai.ocs.mapper.RefreshTokenMapper;
import com.bohuai.ocs.mapper.UserMapper;
import com.bohuai.ocs.model.RefreshToken;
import com.bohuai.ocs.model.User;
import com.bohuai.ocs.security.CurrentUser;
import com.bohuai.ocs.security.JwtService;
import com.bohuai.ocs.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Session management: a short-lived JWT access token plus a long-lived, single-use refresh token.
 * <ul>
 *   <li>Refresh tokens are random 256-bit values; only their SHA-256 hash is stored.</li>
 *   <li>Every refresh rotates the token. Presenting a token that was already rotated means it was
 *       copied (stolen or replayed), so all of that user's sessions are revoked — except within a short
 *       grace period after rotation, which absorbs a browser firing several requests at once.</li>
 *   <li>Logout, password change and role change revoke refresh tokens; outstanding access tokens
 *       expire within {@code ocs.jwt.access-ttl-seconds}.</li>
 * </ul>
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record TokenPair(String accessToken, long accessTtlSeconds, String refreshToken,
                            long refreshTtlSeconds, User user) {
    }

    private final RefreshTokenMapper tokens;
    private final UserMapper users;
    private final JwtService jwt;
    private final long refreshTtlSeconds;
    private final Duration reuseGrace;

    public TokenService(RefreshTokenMapper tokens, UserMapper users, JwtService jwt, OcsProperties props) {
        this.tokens = tokens;
        this.users = users;
        this.jwt = jwt;
        this.refreshTtlSeconds = props.jwt().refreshTtlSeconds();
        this.reuseGrace = Duration.ofSeconds(props.jwt().reuseGraceSeconds());
    }

    @Transactional
    public TokenPair issue(User user) {
        return issueInternal(user).pair();
    }

    // noRollbackFor: the reuse branch revokes every session and then rejects the request; rolling back
    // on that rejection would silently undo the revocation.
    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized("Session expired, please sign in again");
        }
        RefreshToken current = tokens.findByHashForUpdate(hash(rawRefreshToken));
        if (current == null) {
            throw ApiException.unauthorized("Session expired, please sign in again");
        }
        if (current.getRevokedAt() != null) {
            boolean justRotated = current.getReplacedBy() != null
                    && current.getRevokedAt().plus(reuseGrace).isAfter(Instant.now());
            if (justRotated) {
                // Concurrent refresh from the same browser (e.g. page + prefetch): give it its own session.
                User user = users.findById(current.getUserId());
                if (user != null) {
                    return issueInternal(user).pair();
                }
            }
            if (current.getReplacedBy() != null) {
                log.warn("Refresh token reuse detected for user {}; revoking all sessions", current.getUserId());
                tokens.revokeAllForUser(current.getUserId());
            }
            throw ApiException.unauthorized("Session expired, please sign in again");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Session expired, please sign in again");
        }
        User user = users.findById(current.getUserId());
        if (user == null) {
            throw ApiException.unauthorized("Account no longer exists");
        }
        Issued next = issueInternal(user);
        tokens.markReplaced(current.getId(), next.refreshId());
        return next.pair();
    }

    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            tokens.revokeByHash(hash(rawRefreshToken));
        }
    }

    public void revokeAll(long userId) {
        tokens.revokeAllForUser(userId);
    }

    /** Housekeeping: drop refresh tokens that expired more than a day ago. */
    @Scheduled(cron = "${ocs.jwt.purge-cron:0 30 3 * * *}")
    public void purgeExpired() {
        int n = tokens.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        if (n > 0) {
            log.info("Purged {} expired refresh tokens", n);
        }
    }

    private record Issued(TokenPair pair, long refreshId) {
    }

    private Issued issueInternal(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(hash(raw));
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        tokens.insert(rt);
        String access = jwt.issue(new CurrentUser(user.getId(), user.getDisplayName(), Role.valueOf(user.getRole())));
        return new Issued(new TokenPair(access, jwt.ttlSeconds(), raw, refreshTtlSeconds, user), rt.getId());
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
