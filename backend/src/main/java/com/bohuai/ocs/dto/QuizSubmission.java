package com.bohuai.ocs.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** questionId -> chosen optionId. Unanswered questions count as wrong. */
public record QuizSubmission(@NotNull Map<Long, Long> answers) {
}
