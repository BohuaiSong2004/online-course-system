package com.bohuai.ocs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 200000) String content,
        @Min(1) @Max(600) Integer durationMin,
        Boolean preview) {
}
