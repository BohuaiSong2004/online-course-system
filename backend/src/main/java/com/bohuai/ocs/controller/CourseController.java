package com.bohuai.ocs.controller;

import com.bohuai.ocs.common.PageResult;
import com.bohuai.ocs.dto.CourseDetail;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalog (no sign-in needed). */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courses;
    private final Auth auth;

    public CourseController(CourseService courses, Auth auth) {
        this.courses = courses;
        this.auth = auth;
    }

    @GetMapping
    public PageResult<Course> search(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String level,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "12") int size) {
        return courses.search(q, category, level, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return courses.categories();
    }

    @GetMapping("/sitemap")
    public List<Course> sitemap() {
        return courses.sitemap();
    }

    @GetMapping("/{slug}")
    public CourseDetail detail(@PathVariable String slug) {
        return courses.detail(slug, auth.current());
    }
}
