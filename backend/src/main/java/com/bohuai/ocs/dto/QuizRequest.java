package com.bohuai.ocs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Replaces a chapter's whole quiz. An empty question list removes the quiz. */
public record QuizRequest(
        @NotNull @Min(1) @Max(100) Integer passPercent,
        @NotNull @Size(max = 30) List<@Valid Question> questions) {

    public record Question(@NotBlank @Size(max = 1000) String prompt,
                           @Size(max = 2000) String explanation,
                           @NotNull @Size(min = 2, max = 6) List<@Valid Option> options) {
    }

    public record Option(@NotBlank @Size(max = 500) String text, boolean correct) {
    }
}
