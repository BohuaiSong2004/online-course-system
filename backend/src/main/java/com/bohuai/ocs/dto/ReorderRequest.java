package com.bohuai.ocs.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequest(@NotEmpty List<Long> chapterIds) {
}
