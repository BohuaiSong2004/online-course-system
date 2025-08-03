package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.dto.AssignRequest;
import com.bohuai.ocs.dto.AssignResult;
import com.bohuai.ocs.dto.ReportOverview;
import com.bohuai.ocs.mapper.AdminMapper;
import com.bohuai.ocs.model.AssignmentRow;
import com.bohuai.ocs.model.Course;
import com.bohuai.ocs.model.CourseStat;
import com.bohuai.ocs.model.TeamStat;
import com.bohuai.ocs.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Course assignments with due dates, and the completion reports managers and admins use. */
@Service
public class AssignmentService {

    public static final String COMPLETED = "COMPLETED";
    public static final String OVERDUE = "OVERDUE";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String NOT_STARTED = "NOT_STARTED";
    private static final int MAX_ROWS = 5000;

    private final AdminMapper admin;
    private final CourseService courses;
    private final ActivityService activity;
    private final Clock clock;

    @Autowired
    public AssignmentService(AdminMapper admin, CourseService courses, ActivityService activity) {
        this(admin, courses, activity, Clock.systemUTC());
    }

    AssignmentService(AdminMapper admin, CourseService courses, ActivityService activity, Clock clock) {
        this.admin = admin;
        this.courses = courses;
        this.activity = activity;
        this.clock = clock;
    }

    /** Assigns (and enrolls) every requested user the caller is allowed to manage. Idempotent. */
    @Transactional
    public AssignResult assign(CurrentUser caller, AssignRequest req) {
        Course course = courses.requireById(req.courseId());
        if (!CourseService.PUBLISHED.equals(course.getStatus())) {
            throw ApiException.badRequest("Only published courses can be assigned");
        }
        if (req.dueDate() != null && req.dueDate().isBefore(today())) {
            throw ApiException.badRequest("Due date can't be in the past");
        }
        Long scope = AdminService.scope(caller);
        Set<Long> requested = new LinkedHashSet<>();
        if (req.userIds() != null) {
            requested.addAll(req.userIds());
        }
        if (req.teamId() != null) {
            if (admin.findTeam(req.teamId()) == null) {
                throw ApiException.badRequest("Team does not exist");
            }
            requested.addAll(admin.teamMemberIds(req.teamId(), scope));
        }
        requested.remove(null);
        if (requested.isEmpty()) {
            throw ApiException.badRequest("Choose at least one user or a team with members");
        }
        List<Long> allowed = admin.usersInScope(new ArrayList<>(requested), scope);
        int created = 0, updated = 0, unchanged = 0;
        for (Long userId : allowed) {
            int n = admin.upsertAssignment(userId, course.getId(), caller.id(), req.dueDate());
            if (n == 1) {
                created++;
                activity.log(userId, course.getId(), null, ActivityService.ASSIGNED);
            } else if (n == 2) {
                updated++;
            } else {
                unchanged++;
            }
            admin.enrollIfMissing(userId, course.getId());
        }
        return new AssignResult(created, updated, unchanged, requested.size() - allowed.size());
    }

    public List<AssignmentRow> list(CurrentUser caller, Long courseId, Long teamId, Long userId, String status) {
        List<AssignmentRow> rows = admin.listAssignments(AdminService.scope(caller), courseId, teamId, userId, MAX_ROWS);
        rows.forEach(this::decorate);
        if (status != null && !status.isBlank()) {
            rows.removeIf(r -> !status.equalsIgnoreCase(r.getStatus()));
        }
        return rows;
    }

    @Transactional
    public void remove(CurrentUser caller, long assignmentId) {
        if (admin.findAssignment(assignmentId, AdminService.scope(caller)) == null) {
            throw ApiException.notFound("Assignment");
        }
        admin.deleteAssignment(assignmentId); // the enrollment and progress are kept
    }

    public ReportOverview overview(CurrentUser caller) {
        Long scope = AdminService.scope(caller);
        List<CourseStat> courseStats = admin.courseStats(scope);
        courseStats.forEach(c -> c.setCompletionRate(rate(c.getCompleted(), c.getEnrolled())));
        List<TeamStat> teamStats = admin.teamStats(scope);
        teamStats.forEach(t -> t.setCompletionRate(rate(t.getCompleted(), t.getAssigned())));
        List<AssignmentRow> rows = list(caller, null, null, null, null);
        int done = (int) rows.stream().filter(r -> COMPLETED.equals(r.getStatus())).count();
        int overdue = (int) rows.stream().filter(r -> OVERDUE.equals(r.getStatus())).count();
        return new ReportOverview(admin.countLearnersInScope(scope), rows.size(), done, overdue,
                rate(done, rows.size()), courseStats, teamStats);
    }

    /** RFC 4180 CSV of every assignment in scope, with formula-injection protection for spreadsheets. */
    public String csv(CurrentUser caller) {
        StringBuilder sb = new StringBuilder(
                "learner,email,team,course,status,progress_percent,due_date,assigned_at,completed_at,assigned_by\r\n");
        DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;
        for (AssignmentRow r : list(caller, null, null, null, null)) {
            int pct = r.getTotalChapters() == 0 ? 0 : Math.round(100f * r.getCompletedChapters() / r.getTotalChapters());
            sb.append(String.join(",",
                    cell(r.getUserName()), cell(r.getUserEmail()), cell(r.getTeamName()), cell(r.getCourseTitle()),
                    cell(r.getStatus()), cell(String.valueOf(pct)),
                    cell(r.getDueDate() == null ? "" : r.getDueDate().toString()),
                    cell(r.getAssignedAt() == null ? "" : iso.format(r.getAssignedAt())),
                    cell(r.getCompletedAt() == null ? "" : iso.format(r.getCompletedAt())),
                    cell(r.getAssignedByName()))).append("\r\n");
        }
        return sb.toString();
    }

    void decorate(AssignmentRow r) {
        String status;
        if (COMPLETED.equals(r.getEnrollmentStatus())) {
            status = COMPLETED;
        } else if (r.getDueDate() != null && r.getDueDate().isBefore(today())) {
            status = OVERDUE;
        } else if (r.getCompletedChapters() == null || r.getCompletedChapters() == 0) {
            status = NOT_STARTED;
        } else {
            status = IN_PROGRESS;
        }
        r.setStatus(status);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    private static double rate(Integer done, Integer total) {
        if (total == null || total == 0 || done == null) {
            return 0;
        }
        return Math.round(1000.0 * done / total) / 10.0; // one decimal place, as a percentage
    }

    static String cell(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s; // stop spreadsheets from evaluating the cell as a formula
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
