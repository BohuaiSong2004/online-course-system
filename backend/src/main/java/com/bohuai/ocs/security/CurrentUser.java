package com.bohuai.ocs.security;

/** The authenticated caller, decoded from the JWT. */
public record CurrentUser(long id, String displayName, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
