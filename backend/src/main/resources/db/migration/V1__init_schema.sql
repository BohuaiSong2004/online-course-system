-- Online Course Management System schema (MySQL 8, InnoDB, utf8mb4).
-- Baseline schema. Applied once by Flyway.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'LEARNER',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE courses (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    slug          VARCHAR(160) NOT NULL,
    title         VARCHAR(150) NOT NULL,
    summary       VARCHAR(500) NOT NULL DEFAULT '',
    description   TEXT         NULL,
    category      VARCHAR(50)  NOT NULL,
    level         VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    instructor_id BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at  DATETIME(3)  NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_slug (slug),
    KEY idx_courses_status_published (status, published_at),
    KEY idx_courses_category_status (category, status),
    KEY idx_courses_instructor (instructor_id),
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chapters (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    course_id    BIGINT       NOT NULL,
    title        VARCHAR(150) NOT NULL,
    content      MEDIUMTEXT   NULL,
    position     INT          NOT NULL,
    duration_min INT          NOT NULL DEFAULT 10,
    is_preview   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_chapters_course_position (course_id, position),
    CONSTRAINT fk_chapters_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE enrollments (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    course_id    BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    -- B-tree on (user_id, course_id): serves "my courses" lookups by user and enforces one enrollment per course
    UNIQUE KEY uk_enrollments_user_course (user_id, course_id),
    KEY idx_enrollments_course (course_id),
    CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chapter_progress (
    user_id      BIGINT      NOT NULL,
    chapter_id   BIGINT      NOT NULL,
    course_id    BIGINT      NOT NULL,
    completed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, chapter_id),
    KEY idx_progress_user_course (user_id, course_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Append-only activity log. Hot query: latest N events for one user.
-- The composite B-tree (user_id, created_at) turns it into an index range scan with no filesort.
CREATE TABLE user_activity (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    course_id  BIGINT      NULL,
    chapter_id BIGINT      NULL,
    action     VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_activity_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
