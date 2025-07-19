package com.bohuai.ocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        @NotBlank @Pattern(regexp = "LEARNER|MANAGER|INSTRUCTOR|ADMIN") String role,
        Long teamId,
        @NotNull Boolean active) {
}
