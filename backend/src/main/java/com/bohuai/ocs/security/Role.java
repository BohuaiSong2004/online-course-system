package com.bohuai.ocs.security;

public enum Role {
    LEARNER,
    INSTRUCTOR,
    ADMIN;

    public boolean canAuthor() {
        return this == INSTRUCTOR || this == ADMIN;
    }
}
