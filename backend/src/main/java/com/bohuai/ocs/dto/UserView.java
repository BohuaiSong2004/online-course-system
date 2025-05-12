package com.bohuai.ocs.dto;

import com.bohuai.ocs.model.User;

public record UserView(long id, String email, String displayName, String role) {

    public static UserView of(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole());
    }
}
