package com.bohuai.ocs.controller;

import com.bohuai.ocs.dto.ChapterRequest;
import com.bohuai.ocs.dto.CourseDetail;
import com.bohuai.ocs.dto.CourseRequest;
import com.bohuai.ocs.dto.ReorderRequest;
import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.ChapterService;
import com.bohuai.ocs.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Course and chapter authoring. Instructors see their own courses; admins see all. */
@RestController
@RequestMapping("/api/studio")
public class StudioController {

    private final Auth auth;
    private final CourseService courses;
    private final ChapterService chapters;

    public StudioController(Auth auth, CourseService courses, ChapterService chapters) {
        this.auth = auth;
        this.courses = courses;
        this.chapters = chapters;
    }

    @GetMapping("/courses")
    public List<Course> list() {
        return courses.studioList(auth.requireAuthor());
    }

    @GetMapping("/courses/{courseId}")
    public CourseDetail get(@PathVariable long courseId) {
        return courses.studioDetail(auth.requireAuthor(), courseId);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Course create(@Valid @RequestBody CourseRequest req) {
        return courses.create(auth.requireAuthor(), req);
    }

    @PutMapping("/courses/{courseId}")
    public Course update(@PathVariable long courseId, @Valid @RequestBody CourseRequest req) {
        return courses.update(auth.requireAuthor(), courseId, req);
    }

    @PostMapping("/courses/{courseId}/publish")
    public Course publish(@PathVariable long courseId) {
        return courses.setPublished(auth.requireAuthor(), courseId, true);
    }

    @PostMapping("/courses/{courseId}/unpublish")
    public Course unpublish(@PathVariable long courseId) {
        return courses.setPublished(auth.requireAuthor(), courseId, false);
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> delete(@PathVariable long courseId) {
        courses.delete(auth.requireAuthor(), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/courses/{courseId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public Chapter addChapter(@PathVariable long courseId, @Valid @RequestBody ChapterRequest req) {
        return chapters.create(auth.requireAuthor(), courseId, req);
    }

    @PutMapping("/courses/{courseId}/chapters/order")
    public List<Chapter> reorder(@PathVariable long courseId, @Valid @RequestBody ReorderRequest req) {
        return chapters.reorder(auth.requireAuthor(), courseId, req.chapterIds());
    }

    @GetMapping("/chapters/{chapterId}")
    public Chapter getChapter(@PathVariable long chapterId) {
        return chapters.studioGet(auth.requireAuthor(), chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public Chapter updateChapter(@PathVariable long chapterId, @Valid @RequestBody ChapterRequest req) {
        return chapters.update(auth.requireAuthor(), chapterId, req);
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(@PathVariable long chapterId) {
        chapters.delete(auth.requireAuthor(), chapterId);
        return ResponseEntity.noContent().build();
    }
}
