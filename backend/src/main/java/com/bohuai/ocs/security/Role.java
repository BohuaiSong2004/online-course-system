package com.bohuai.ocs.security;

public enum Role {
    LEARNER,
    /** Assigns courses to, and reports on, the members of the teams they manage. */
    MANAGER,
    INSTRUCTOR,
    ADMIN;

    public boolean canAuthor() {
        return this == INSTRUCTOR || this == ADMIN;
    }

    public boolean canManage() {
        return this == MANAGER || this == ADMIN;
    }
}
