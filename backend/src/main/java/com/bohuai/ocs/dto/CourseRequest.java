package com.bohuai.ocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 500) String summary,
        @Size(max = 20000) String description,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED") String level) {
}
