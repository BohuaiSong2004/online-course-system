package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.common.PageResult;
import com.bohuai.ocs.dto.CourseDetail;
import com.bohuai.ocs.dto.CourseRequest;
import com.bohuai.ocs.dto.ViewerState;
import com.bohuai.ocs.mapper.ChapterMapper;
import com.bohuai.ocs.mapper.CourseMapper;
import com.bohuai.ocs.mapper.EnrollmentMapper;
import com.bohuai.ocs.mapper.ProgressMapper;
import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.model.Enrollment;
import com.bohuai.ocs.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CourseService {

    public static final String PUBLISHED = "PUBLISHED";
    public static final String DRAFT = "DRAFT";
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE = 10_000; // keeps (page - 1) * size far from int overflow

    private final CourseMapper courses;
    private final ChapterMapper chapters;
    private final EnrollmentMapper enrollments;
    private final ProgressMapper progress;

    public CourseService(CourseMapper courses, ChapterMapper chapters, EnrollmentMapper enrollments,
                         ProgressMapper progress) {
        this.courses = courses;
        this.chapters = chapters;
        this.enrollments = enrollments;
        this.progress = progress;
    }

    // ---------- public catalog ----------

    public PageResult<Course> search(String q, String category, String level, int page, int size) {
        int p = Math.max(1, Math.min(page, MAX_PAGE));
        int s = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        String query = q == null ? null : escapeLike(q.trim());
        List<Course> items = courses.searchPublished(query, blankToNull(category), blankToNull(level), (p - 1) * s, s);
        long total = courses.countPublished(query, blankToNull(category), blankToNull(level));
        return PageResult.of(items, p, s, total);
    }

    public List<String> categories() {
        return courses.listPublishedCategories();
    }

    public List<Course> sitemap() {
        return courses.listPublishedForSitemap();
    }

    public CourseDetail detail(String slug, Optional<CurrentUser> viewer) {
        Course c = courses.findBySlug(slug);
        if (c == null) {
            throw ApiException.notFound("Course");
        }
        boolean canEdit = viewer.map(v -> canEdit(v, c)).orElse(false);
        if (!PUBLISHED.equals(c.getStatus()) && !canEdit) {
            throw ApiException.notFound("Course"); // drafts are invisible, not forbidden
        }
        List<Chapter> outline = chapters.findOutlineByCourse(c.getId());
        ViewerState state = viewer.map(v -> {
            Enrollment e = enrollments.find(v.id(), c.getId());
            List<Long> done = e == null ? List.<Long>of() : progress.completedChapterIds(v.id(), c.getId());
            return new ViewerState(true, e != null, e == null ? null : e.getStatus(), canEdit, done);
        }).orElse(ViewerState.anonymous());
        return new CourseDetail(c, outline, enrollments.countByCourse(c.getId()), state);
    }

    // ---------- studio (instructors / admins) ----------

    public List<Course> studioList(CurrentUser author) {
        return author.isAdmin() ? courses.findAllForAdmin() : courses.findByInstructor(author.id());
    }

    public CourseDetail studioDetail(CurrentUser author, long courseId) {
        Course c = requireEditable(author, courseId);
        return new CourseDetail(c, chapters.findOutlineByCourse(courseId), enrollments.countByCourse(courseId),
                new ViewerState(true, false, null, true, List.of()));
    }

    @Transactional
    public Course create(CurrentUser author, CourseRequest req) {
        Course c = new Course();
        apply(c, req);
        c.setInstructorId(author.id());
        c.setSlug(uniqueSlug(req.title()));
        courses.insert(c);
        return courses.findById(c.getId());
    }

    @Transactional
    public Course update(CurrentUser author, long courseId, CourseRequest req) {
        Course c = requireEditable(author, courseId);
        apply(c, req);
        courses.update(c);
        return courses.findById(courseId);
    }

    @Transactional
    public Course setPublished(CurrentUser author, long courseId, boolean publish) {
        Course c = requireEditable(author, courseId);
        if (publish && (c.getChapterCount() == null || c.getChapterCount() == 0)) {
            throw ApiException.badRequest("Add at least one chapter before publishing");
        }
        courses.updateStatus(courseId, publish ? PUBLISHED : DRAFT);
        return courses.findById(courseId);
    }

    @Transactional
    public void delete(CurrentUser author, long courseId) {
        requireEditable(author, courseId);
        courses.delete(courseId);
    }

    public Course requireEditable(CurrentUser author, long courseId) {
        Course c = courses.findById(courseId);
        if (c == null) {
            throw ApiException.notFound("Course");
        }
        if (!canEdit(author, c)) {
            throw ApiException.forbidden("You can only edit your own courses");
        }
        return c;
    }

    public Course requireById(long courseId) {
        Course c = courses.findById(courseId);
        if (c == null) {
            throw ApiException.notFound("Course");
        }
        return c;
    }

    static boolean canEdit(CurrentUser u, Course c) {
        return u.isAdmin() || (u.role().canAuthor() && c.getInstructorId() != null && c.getInstructorId() == u.id());
    }

    private static void apply(Course c, CourseRequest req) {
        c.setTitle(req.title().trim());
        c.setSummary(req.summary() == null ? "" : req.summary().trim());
        c.setDescription(req.description());
        c.setCategory(req.category().trim());
        c.setLevel(req.level());
    }

    private String uniqueSlug(String title) {
        String base = slugify(title);
        String slug = base;
        for (int i = 2; courses.slugExists(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    static String slugify(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (s.length() > 120) {
            s = s.substring(0, 120).replaceAll("-+$", "");
        }
        return s.isEmpty() ? "course" : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String escapeLike(String s) {
        if (s.isEmpty()) {
            return null;
        }
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
