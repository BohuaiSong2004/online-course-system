package com.bohuai.ocs.dto;

import com.bohuai.ocs.model.Chapter;

public record ChapterView(Chapter chapter, String courseSlug, String courseTitle,
                          Long prevChapterId, Long nextChapterId, boolean completed, boolean enrolled) {
}
