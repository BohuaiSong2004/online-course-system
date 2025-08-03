-- Richer content: per-chapter video, chapter quizzes with a pass mark, completion certificates,
-- and full-text search over the catalog.

ALTER TABLE chapters
    ADD COLUMN video_url         VARCHAR(500) NULL AFTER content,
    ADD COLUMN quiz_pass_percent INT          NOT NULL DEFAULT 70 AFTER is_preview;

CREATE TABLE quiz_questions (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    chapter_id  BIGINT        NOT NULL,
    position    INT           NOT NULL,
    prompt      VARCHAR(1000) NOT NULL,
    explanation VARCHAR(2000) NULL,
    PRIMARY KEY (id),
    KEY idx_quiz_questions_chapter (chapter_id, position),
    CONSTRAINT fk_quiz_questions_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quiz_options (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    question_id BIGINT       NOT NULL,
    position    INT          NOT NULL,
    text        VARCHAR(500) NOT NULL,
    correct     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_quiz_options_question (question_id, position),
    CONSTRAINT fk_quiz_options_question FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quiz_attempts (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    chapter_id BIGINT      NOT NULL,
    score      INT         NOT NULL,
    total      INT         NOT NULL,
    passed     BOOLEAN     NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quiz_attempts_user_chapter (user_id, chapter_id, created_at),
    CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempts_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- One certificate per learner and course; the public code lets anyone verify it.
CREATE TABLE certificates (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id   BIGINT      NOT NULL,
    course_id BIGINT      NOT NULL,
    code      CHAR(12)    NOT NULL,
    issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificates_code (code),
    UNIQUE KEY uk_certificates_user_course (user_id, course_id),
    CONSTRAINT fk_certificates_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_certificates_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE courses ADD FULLTEXT INDEX ft_courses_text (title, summary, description);
