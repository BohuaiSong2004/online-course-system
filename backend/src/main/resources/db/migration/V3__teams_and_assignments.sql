-- Corporate training: teams with a manager, course assignments with due dates, deactivatable users.

CREATE TABLE teams (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    manager_id BIGINT       NULL,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_teams_name (name),
    KEY idx_teams_manager (manager_id),
    CONSTRAINT fk_teams_manager FOREIGN KEY (manager_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN team_id BIGINT  NULL AFTER role,
    ADD COLUMN active  BOOLEAN NOT NULL DEFAULT TRUE AFTER team_id,
    ADD KEY idx_users_team (team_id),
    ADD CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL;

-- One assignment per (user, course). Assigning also enrolls the user.
CREATE TABLE assignments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    course_id   BIGINT      NOT NULL,
    assigned_by BIGINT      NULL,
    due_date    DATE        NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignments_user_course (user_id, course_id),
    KEY idx_assignments_course (course_id),
    KEY idx_assignments_due (due_date),
    CONSTRAINT fk_assignments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_by FOREIGN KEY (assigned_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
