package com.bohuai.ocs.security;

import com.bohuai.ocs.config.OcsProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal stateless HS256 JWT implementation (issue + verify). No server-side session state,
 * so any API instance behind the gateway can validate a token.
 */
@Service
public class JwtService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] secret;
    private final long ttlSeconds;
    private final ObjectMapper mapper;

    public JwtService(OcsProperties props, ObjectMapper mapper) {
        String s = props.jwt().secret();
        if (s == null || s.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ocs.jwt.secret must be at least 32 bytes");
        }
        this.secret = s.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = props.jwt().ttlSeconds();
        this.mapper = mapper;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String issue(CurrentUser user) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.id()));
        claims.put("name", user.displayName());
        claims.put("role", user.role().name());
        claims.put("iat", now);
        claims.put("exp", now + ttlSeconds);
        try {
            String payload = B64.encodeToString(mapper.writeValueAsBytes(claims));
            String signingInput = HEADER + "." + payload;
            return signingInput + "." + B64.encodeToString(sign(signingInput));
        } catch (Exception e) {
            throw new IllegalStateException("Could not issue token", e);
        }
    }

    public Optional<CurrentUser> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !HEADER.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] actual = B64D.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                return Optional.empty();
            }
            Map<String, Object> claims = mapper.readValue(B64D.decode(parts[1]), new TypeReference<Map<String, Object>>() {
            });
            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            return Optional.of(new CurrentUser(
                    Long.parseLong((String) claims.get("sub")),
                    (String) claims.get("name"),
                    Role.valueOf((String) claims.get("role"))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }
}
