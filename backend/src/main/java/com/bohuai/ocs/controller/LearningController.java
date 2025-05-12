package com.bohuai.ocs.controller;

import com.bohuai.ocs.dto.ChapterView;
import com.bohuai.ocs.dto.ProgressResponse;
import com.bohuai.ocs.model.Activity;
import com.bohuai.ocs.model.Enrollment;
import com.bohuai.ocs.model.EnrollmentView;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.ActivityService;
import com.bohuai.ocs.service.ChapterService;
import com.bohuai.ocs.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Enrollment, chapter reading, progress and activity for signed-in learners. */
@RestController
@RequestMapping("/api")
public class LearningController {

    private final Auth auth;
    private final EnrollmentService enrollments;
    private final ChapterService chapters;
    private final ActivityService activity;

    public LearningController(Auth auth, EnrollmentService enrollments, ChapterService chapters,
                              ActivityService activity) {
        this.auth = auth;
        this.enrollments = enrollments;
        this.chapters = chapters;
        this.activity = activity;
    }

    @PostMapping("/courses/{courseId}/enroll")
    public Enrollment enroll(@PathVariable long courseId) {
        return enrollments.enroll(auth.require(), courseId);
    }

    @DeleteMapping("/courses/{courseId}/enroll")
    public ResponseEntity<Void> unenroll(@PathVariable long courseId) {
        enrollments.unenroll(auth.require(), courseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterView read(@PathVariable long chapterId) {
        return chapters.read(chapterId, auth.current());
    }

    @PostMapping("/chapters/{chapterId}/complete")
    public ProgressResponse complete(@PathVariable long chapterId) {
        return chapters.complete(chapterId, auth.require());
    }

    @GetMapping("/me/enrollments")
    public List<EnrollmentView> myEnrollments() {
        return enrollments.mine(auth.require());
    }

    @GetMapping("/me/activity")
    public List<Activity> myActivity(@RequestParam(defaultValue = "20") int limit) {
        return activity.recent(auth.require().id(), limit);
    }
}
