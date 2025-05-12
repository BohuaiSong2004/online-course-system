package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.dto.ChapterRequest;
import com.bohuai.ocs.dto.ChapterView;
import com.bohuai.ocs.dto.ProgressResponse;
import com.bohuai.ocs.mapper.ChapterMapper;
import com.bohuai.ocs.mapper.EnrollmentMapper;
import com.bohuai.ocs.mapper.ProgressMapper;
import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.model.Enrollment;
import com.bohuai.ocs.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ChapterService {

    private final ChapterMapper chapters;
    private final EnrollmentMapper enrollments;
    private final ProgressMapper progress;
    private final CourseService courseService;
    private final ActivityService activity;

    public ChapterService(ChapterMapper chapters, EnrollmentMapper enrollments, ProgressMapper progress,
                          CourseService courseService, ActivityService activity) {
        this.chapters = chapters;
        this.enrollments = enrollments;
        this.progress = progress;
        this.courseService = courseService;
        this.activity = activity;
    }

    // ---------- learner ----------

    /**
     * Returns a chapter's content if the caller may read it: preview chapters of published courses are
     * public; everything else requires an enrollment (or edit rights on the course).
     */
    public ChapterView read(long chapterId, Optional<CurrentUser> viewer) {
        Chapter ch = requireChapter(chapterId);
        Course course = courseService.requireById(ch.getCourseId());
        boolean canEdit = viewer.map(v -> CourseService.canEdit(v, course)).orElse(false);
        boolean published = CourseService.PUBLISHED.equals(course.getStatus());
        if (!published && !canEdit) {
            throw ApiException.notFound("Chapter");
        }
        Enrollment e = viewer.map(v -> enrollments.find(v.id(), course.getId())).orElse(null);
        boolean preview = Boolean.TRUE.equals(ch.getPreview());
        if (!preview && e == null && !canEdit) {
            throw viewer.isPresent()
                    ? ApiException.forbidden("Enroll in this course to unlock this chapter")
                    : ApiException.unauthorized("Sign in and enroll to unlock this chapter");
        }

        List<Chapter> outline = chapters.findOutlineByCourse(course.getId());
        Long prev = null;
        Long next = null;
        for (int i = 0; i < outline.size(); i++) {
            if (outline.get(i).getId() == chapterId) {
                prev = i > 0 ? outline.get(i - 1).getId() : null;
                next = i < outline.size() - 1 ? outline.get(i + 1).getId() : null;
                break;
            }
        }
        boolean completed = false;
        if (e != null) {
            long uid = viewer.get().id();
            completed = progress.completedChapterIds(uid, course.getId()).contains(chapterId);
            activity.log(uid, course.getId(), chapterId, ActivityService.VIEW_CHAPTER);
        }
        return new ChapterView(ch, course.getSlug(), course.getTitle(), prev, next, completed, e != null);
    }

    @Transactional
    public ProgressResponse complete(long chapterId, CurrentUser user) {
        Chapter ch = requireChapter(chapterId);
        long courseId = ch.getCourseId();
        Course course = courseService.requireById(courseId);
        if (!CourseService.PUBLISHED.equals(course.getStatus()) && !CourseService.canEdit(user, course)) {
            throw ApiException.notFound("Chapter");
        }
        if (enrollments.find(user.id(), courseId) == null) {
            throw ApiException.forbidden("Enroll in this course first");
        }
        if (progress.markComplete(user.id(), chapterId, courseId) > 0) {
            activity.log(user.id(), courseId, chapterId, ActivityService.COMPLETE_CHAPTER);
        }
        List<Chapter> outline = chapters.findOutlineByCourse(courseId);
        Set<Long> done = new HashSet<>(progress.completedChapterIds(user.id(), courseId));
        Long nextId = outline.stream().map(Chapter::getId).filter(id -> !done.contains(id)).findFirst().orElse(null);
        boolean courseDone = !outline.isEmpty() && nextId == null;
        if (courseDone && enrollments.markCompleted(user.id(), courseId) > 0) {
            activity.log(user.id(), courseId, null, ActivityService.COMPLETE_COURSE);
        }
        return new ProgressResponse(done.size(), outline.size(), courseDone, nextId);
    }

    // ---------- studio ----------

    public Chapter studioGet(CurrentUser author, long chapterId) {
        Chapter ch = requireChapter(chapterId);
        courseService.requireEditable(author, ch.getCourseId());
        return ch;
    }

    @Transactional
    public Chapter create(CurrentUser author, long courseId, ChapterRequest req) {
        courseService.requireEditable(author, courseId);
        Chapter ch = new Chapter();
        ch.setCourseId(courseId);
        ch.setPosition(chapters.nextPosition(courseId));
        apply(ch, req);
        chapters.insert(ch);
        return chapters.findById(ch.getId());
    }

    @Transactional
    public Chapter update(CurrentUser author, long chapterId, ChapterRequest req) {
        Chapter ch = studioGet(author, chapterId);
        apply(ch, req);
        chapters.update(ch);
        return chapters.findById(chapterId);
    }

    @Transactional
    public void delete(CurrentUser author, long chapterId) {
        studioGet(author, chapterId);
        chapters.delete(chapterId);
    }

    /** Rewrites positions 1..n in the given order. The list must contain exactly the course's chapters. */
    @Transactional
    public List<Chapter> reorder(CurrentUser author, long courseId, List<Long> orderedIds) {
        courseService.requireEditable(author, courseId);
        List<Chapter> current = chapters.findOutlineByCourse(courseId);
        Set<Long> existing = new HashSet<>();
        current.forEach(c -> existing.add(c.getId()));
        if (orderedIds.size() != existing.size() || !existing.equals(new HashSet<>(orderedIds))) {
            throw ApiException.badRequest("chapterIds must list every chapter of the course exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            chapters.updatePosition(orderedIds.get(i), courseId, i + 1);
        }
        return chapters.findOutlineByCourse(courseId);
    }

    private Chapter requireChapter(long id) {
        Chapter ch = chapters.findById(id);
        if (ch == null) {
            throw ApiException.notFound("Chapter");
        }
        return ch;
    }

    private static void apply(Chapter ch, ChapterRequest req) {
        ch.setTitle(req.title().trim());
        ch.setContent(req.content() == null ? "" : req.content());
        ch.setDurationMin(req.durationMin() == null ? 10 : req.durationMin());
        ch.setPreview(Boolean.TRUE.equals(req.preview()));
    }
}
