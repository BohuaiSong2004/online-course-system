-- Rotating refresh tokens. Only a SHA-256 hash of each token is stored, so a database leak does not
-- leak usable credentials. replaced_by links a rotated token to its successor; presenting an already
-- rotated token is treated as theft and revokes every session of that user.
CREATE TABLE refresh_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  CHAR(64)    NOT NULL,
    expires_at  DATETIME(3) NOT NULL,
    revoked_at  DATETIME(3) NULL,
    replaced_by BIGINT      NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
