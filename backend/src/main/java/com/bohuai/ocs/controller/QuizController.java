package com.bohuai.ocs.controller;

import com.bohuai.ocs.dto.QuizRequest;
import com.bohuai.ocs.dto.QuizResult;
import com.bohuai.ocs.dto.QuizSubmission;
import com.bohuai.ocs.model.Certificate;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.CertificateService;
import com.bohuai.ocs.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Quizzes (learner attempts, studio editing) and completion certificates. */
@RestController
@RequestMapping("/api")
public class QuizController {

    private final Auth auth;
    private final QuizService quizzes;
    private final CertificateService certificates;

    public QuizController(Auth auth, QuizService quizzes, CertificateService certificates) {
        this.auth = auth;
        this.quizzes = quizzes;
        this.certificates = certificates;
    }

    @PostMapping("/chapters/{chapterId}/quiz/attempts")
    public QuizResult attempt(@PathVariable long chapterId, @Valid @RequestBody QuizSubmission req) {
        return quizzes.submit(auth.require(), chapterId, req.answers());
    }

    @GetMapping("/studio/chapters/{chapterId}/quiz")
    public Map<String, Object> getQuiz(@PathVariable long chapterId) {
        return quizzes.forEditor(auth.requireAuthor(), chapterId);
    }

    @PutMapping("/studio/chapters/{chapterId}/quiz")
    public Map<String, Object> saveQuiz(@PathVariable long chapterId, @Valid @RequestBody QuizRequest req) {
        return quizzes.save(auth.requireAuthor(), chapterId, req);
    }

    /** Public: anyone holding the code can verify a certificate. */
    @GetMapping("/certificates/{code}")
    public Certificate verify(@PathVariable String code) {
        return certificates.verify(code);
    }

    @GetMapping("/me/certificates")
    public List<Certificate> mine() {
        return certificates.mine(auth.require().id());
    }
}
