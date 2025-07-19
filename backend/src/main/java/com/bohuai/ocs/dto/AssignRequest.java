package com.bohuai.ocs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Assign a course to individual users and/or every member of a team. */
public record AssignRequest(@NotNull Long courseId, @Size(max = 500) List<Long> userIds, Long teamId,
                            LocalDate dueDate) {
}
