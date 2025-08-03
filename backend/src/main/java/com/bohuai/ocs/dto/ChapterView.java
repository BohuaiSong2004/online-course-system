package com.bohuai.ocs.dto;

import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.QuizAttempt;
import com.bohuai.ocs.model.QuizQuestion;

import java.util.List;

/** A chapter as a learner sees it. {@code quiz} never contains the correct-answer flags. */
public record ChapterView(Chapter chapter, String courseSlug, String courseTitle,
                          Long prevChapterId, Long nextChapterId, boolean completed, boolean enrolled,
                          List<QuizQuestion> quiz, QuizAttempt bestAttempt) {
}
