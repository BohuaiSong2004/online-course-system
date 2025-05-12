package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.mapper.EnrollmentMapper;
import com.bohuai.ocs.mapper.ProgressMapper;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.model.Enrollment;
import com.bohuai.ocs.model.EnrollmentView;
import com.bohuai.ocs.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentMapper enrollments;
    private final ProgressMapper progress;
    private final CourseService courseService;
    private final ActivityService activity;

    public EnrollmentService(EnrollmentMapper enrollments, ProgressMapper progress, CourseService courseService,
                             ActivityService activity) {
        this.enrollments = enrollments;
        this.progress = progress;
        this.courseService = courseService;
        this.activity = activity;
    }

    /** Idempotent: enrolling twice returns the existing enrollment. */
    @Transactional
    public Enrollment enroll(CurrentUser user, long courseId) {
        Course c = courseService.requireById(courseId);
        if (!CourseService.PUBLISHED.equals(c.getStatus())) {
            throw ApiException.notFound("Course");
        }
        Enrollment existing = enrollments.find(user.id(), courseId);
        if (existing != null) {
            return existing;
        }
        Enrollment e = new Enrollment();
        e.setUserId(user.id());
        e.setCourseId(courseId);
        try {
            enrollments.insert(e);
        } catch (DuplicateKeyException race) {
            return enrollments.find(user.id(), courseId);
        }
        activity.log(user.id(), courseId, null, ActivityService.ENROLL);
        return enrollments.find(user.id(), courseId);
    }

    @Transactional
    public void unenroll(CurrentUser user, long courseId) {
        if (enrollments.delete(user.id(), courseId) > 0) {
            progress.deleteForCourse(user.id(), courseId);
            activity.log(user.id(), courseId, null, ActivityService.UNENROLL);
        }
    }

    public List<EnrollmentView> mine(CurrentUser user) {
        return enrollments.listForUser(user.id());
    }
}
