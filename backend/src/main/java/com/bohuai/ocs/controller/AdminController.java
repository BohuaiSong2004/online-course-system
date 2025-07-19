package com.bohuai.ocs.controller;

import com.bohuai.ocs.common.PageResult;
import com.bohuai.ocs.dto.AssignRequest;
import com.bohuai.ocs.dto.AssignResult;
import com.bohuai.ocs.dto.ReportOverview;
import com.bohuai.ocs.dto.TeamRequest;
import com.bohuai.ocs.dto.UpdateUserRequest;
import com.bohuai.ocs.model.AdminUserView;
import com.bohuai.ocs.model.AssignmentRow;
import com.bohuai.ocs.model.Team;
import com.bohuai.ocs.security.Auth;
import com.bohuai.ocs.service.AdminService;
import com.bohuai.ocs.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Corporate administration. Admins manage users and teams; managers and admins assign courses
 * and read reports (managers only see members of teams they manage).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Auth auth;
    private final AdminService admin;
    private final AssignmentService assignments;

    public AdminController(Auth auth, AdminService admin, AssignmentService assignments) {
        this.auth = auth;
        this.admin = admin;
        this.assignments = assignments;
    }

    // ---------- users (admin) ----------

    @GetMapping("/users")
    public PageResult<AdminUserView> users(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Long teamId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        auth.requireAdmin();
        return admin.users(q, role, teamId, page, size);
    }

    @PutMapping("/users/{userId}")
    public AdminUserView updateUser(@PathVariable long userId, @Valid @RequestBody UpdateUserRequest req) {
        return admin.updateUser(auth.requireAdmin(), userId, req);
    }

    // ---------- teams ----------

    @GetMapping("/teams")
    public List<Team> teams() {
        return admin.teams(auth.requireManager());
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public Team createTeam(@Valid @RequestBody TeamRequest req) {
        auth.requireAdmin();
        return admin.createTeam(req);
    }

    @PutMapping("/teams/{teamId}")
    public Team updateTeam(@PathVariable long teamId, @Valid @RequestBody TeamRequest req) {
        auth.requireAdmin();
        return admin.updateTeam(teamId, req);
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable long teamId) {
        auth.requireAdmin();
        admin.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }

    // ---------- assignments (manager/admin) ----------

    @PostMapping("/assignments")
    public AssignResult assign(@Valid @RequestBody AssignRequest req) {
        return assignments.assign(auth.requireManager(), req);
    }

    @GetMapping("/assignments")
    public List<AssignmentRow> assignments(@RequestParam(required = false) Long courseId,
                                           @RequestParam(required = false) Long teamId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String status) {
        return assignments.list(auth.requireManager(), courseId, teamId, userId, status);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> unassign(@PathVariable long assignmentId) {
        assignments.remove(auth.requireManager(), assignmentId);
        return ResponseEntity.noContent().build();
    }

    // ---------- reports (manager/admin) ----------

    @GetMapping("/reports/overview")
    public ReportOverview overview() {
        return assignments.overview(auth.requireManager());
    }

    @GetMapping("/reports/assignments.csv")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] body = assignments.csv(auth.requireManager()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"assignments-" + LocalDate.now() + ".csv\"")
                .body(body);
    }
}
