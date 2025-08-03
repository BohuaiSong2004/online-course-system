package com.bohuai.ocs.dto;

import java.util.List;

/**
 * Per-question feedback. Correct answers and explanations are only revealed once the learner
 * passes, so a failed attempt can't simply be retried with the answer key.
 */
public record QuizResult(int score, int total, boolean passed, int passPercent, List<QuestionResult> results,
                         ProgressResponse progress) {

    public record QuestionResult(long questionId, boolean correct, Long correctOptionId, String explanation) {
    }
}
