package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.dto.ProgressResponse;
import com.bohuai.ocs.dto.QuizRequest;
import com.bohuai.ocs.dto.QuizResult;
import com.bohuai.ocs.mapper.ChapterMapper;
import com.bohuai.ocs.mapper.QuizMapper;
import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.QuizAttempt;
import com.bohuai.ocs.model.QuizOption;
import com.bohuai.ocs.model.QuizQuestion;
import com.bohuai.ocs.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chapter quizzes: single-answer multiple choice with a pass mark. A chapter that has a quiz is only
 * completed by passing it; grading always happens server-side and the answer key is never sent to
 * learners before they pass.
 */
@Service
public class QuizService {

    private final QuizMapper quizzes;
    private final ChapterMapper chapters;
    private final ChapterService chapterService;
    private final ActivityService activity;

    public QuizService(QuizMapper quizzes, ChapterMapper chapters, ChapterService chapterService,
                       ActivityService activity) {
        this.quizzes = quizzes;
        this.chapters = chapters;
        this.chapterService = chapterService;
        this.activity = activity;
    }

    /** Questions without the correct-answer flags. */
    public List<QuizQuestion> forLearner(long chapterId) {
        List<QuizQuestion> qs = quizzes.findQuestions(chapterId);
        qs.forEach(q -> q.getOptions().forEach(o -> o.setCorrect(null)));
        return qs;
    }

    public QuizAttempt bestAttempt(long userId, long chapterId) {
        return quizzes.bestAttempt(userId, chapterId);
    }

    // ---------- learner ----------

    @Transactional
    public QuizResult submit(CurrentUser user, long chapterId, Map<Long, Long> answers) {
        Chapter ch = chapterService.requireEnrolledChapter(chapterId, user);
        List<QuizQuestion> questions = quizzes.findQuestions(chapterId);
        if (questions.isEmpty()) {
            throw ApiException.badRequest("This chapter has no quiz");
        }
        int score = 0;
        List<QuizResult.QuestionResult> results = new ArrayList<>();
        List<Long[]> correctIds = new ArrayList<>();
        for (QuizQuestion q : questions) {
            Long chosen = answers.get(q.getId());
            Long correctId = q.getOptions().stream().filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                    .map(QuizOption::getId).findFirst().orElse(null);
            boolean ok = chosen != null && chosen.equals(correctId);
            if (ok) {
                score++;
            }
            correctIds.add(new Long[]{q.getId(), correctId, ok ? 1L : 0L});
        }
        int pass = ch.getQuizPassPercent() == null ? 70 : ch.getQuizPassPercent();
        boolean passed = score * 100 >= pass * questions.size();
        for (int i = 0; i < questions.size(); i++) {
            Long[] c = correctIds.get(i);
            results.add(new QuizResult.QuestionResult(c[0], c[2] == 1L, passed ? c[1] : null,
                    passed ? questions.get(i).getExplanation() : null));
        }

        QuizAttempt a = new QuizAttempt();
        a.setUserId(user.id());
        a.setChapterId(chapterId);
        a.setScore(score);
        a.setTotal(questions.size());
        a.setPassed(passed);
        quizzes.insertAttempt(a);
        activity.log(user.id(), ch.getCourseId(), chapterId, passed ? ActivityService.QUIZ_PASSED : ActivityService.QUIZ_FAILED);

        ProgressResponse progress = passed ? chapterService.markDone(ch, user) : null;
        return new QuizResult(score, questions.size(), passed, pass, results, progress);
    }

    // ---------- studio ----------

    public Map<String, Object> forEditor(CurrentUser author, long chapterId) {
        Chapter ch = chapterService.studioGet(author, chapterId);
        return Map.of("passPercent", ch.getQuizPassPercent(), "questions", quizzes.findQuestions(chapterId));
    }

    @Transactional
    public Map<String, Object> save(CurrentUser author, long chapterId, QuizRequest req) {
        chapterService.studioGet(author, chapterId);
        for (int i = 0; i < req.questions().size(); i++) {
            long correct = req.questions().get(i).options().stream().filter(QuizRequest.Option::correct).count();
            if (correct != 1) {
                throw ApiException.badRequest("Question " + (i + 1) + " must have exactly one correct option");
            }
        }
        quizzes.deleteQuestions(chapterId);
        int qPos = 1;
        for (QuizRequest.Question rq : req.questions()) {
            QuizQuestion q = new QuizQuestion();
            q.setChapterId(chapterId);
            q.setPosition(qPos++);
            q.setPrompt(rq.prompt().trim());
            q.setExplanation(rq.explanation() == null || rq.explanation().isBlank() ? null : rq.explanation().trim());
            quizzes.insertQuestion(q);
            int oPos = 1;
            for (QuizRequest.Option ro : rq.options()) {
                QuizOption o = new QuizOption();
                o.setQuestionId(q.getId());
                o.setPosition(oPos++);
                o.setText(ro.text().trim());
                o.setCorrect(ro.correct());
                quizzes.insertOption(o);
            }
        }
        quizzes.updatePassPercent(chapterId, req.passPercent());
        return forEditor(author, chapterId);
    }
}
