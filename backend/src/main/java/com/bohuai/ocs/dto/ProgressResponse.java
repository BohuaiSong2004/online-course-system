package com.bohuai.ocs.dto;

public record ProgressResponse(int completedChapters, int totalChapters, boolean courseCompleted,
                               Long nextChapterId, String certificateCode) {
}
