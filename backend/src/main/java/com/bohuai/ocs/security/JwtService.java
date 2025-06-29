package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies short-lived HS256 access tokens (jjwt). Stateless: any API instance can
 * verify a token without a database round-trip. Long-lived sessions are handled by
 * {@link com.bohuai.ocs.service.TokenService} with rotating, revocable refresh tokens.
 */
@Service
public class JwtService {

    static final String ISSUER = "ocs";

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(OcsProperties props) {
        String s = props.jwt().secret();
        if (s == null || s.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ocs.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = props.jwt().accessTtlSeconds();
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String issue(CurrentUser user) {
        return issue(user, Instant.now());
    }

    String issue(CurrentUser user, Instant now) {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.id()))
                .claim("name", user.displayName())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<CurrentUser> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new CurrentUser(Long.parseLong(c.getSubject()), c.get("name", String.class),
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
