package com.bohuai.ocs.model;

import java.time.Instant;

/** Server-side record of a refresh token (only its SHA-256 hash is stored). */
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private Long replacedBy;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Long getReplacedBy() { return replacedBy; }
    public void setReplacedBy(Long replacedBy) { this.replacedBy = replacedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
