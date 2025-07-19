package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.common.PageResult;
import com.bohuai.ocs.dto.TeamRequest;
import com.bohuai.ocs.dto.UpdateUserRequest;
import com.bohuai.ocs.mapper.AdminMapper;
import com.bohuai.ocs.mapper.UserMapper;
import com.bohuai.ocs.model.AdminUserView;
import com.bohuai.ocs.model.Team;
import com.bohuai.ocs.model.User;
import com.bohuai.ocs.security.CurrentUser;
import com.bohuai.ocs.security.Role;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** User and team administration. */
@Service
public class AdminService {

    private final AdminMapper admin;
    private final UserMapper users;
    private final TokenService tokens;

    public AdminService(AdminMapper admin, UserMapper users, TokenService tokens) {
        this.admin = admin;
        this.users = users;
        this.tokens = tokens;
    }

    /** null = organisation-wide (admins); otherwise the manager's own teams. */
    public static Long scope(CurrentUser u) {
        return u.isAdmin() ? null : u.id();
    }

    // ---------- users ----------

    public PageResult<AdminUserView> users(String q, String role, Long teamId, int page, int size) {
        int p = Math.max(1, Math.min(page, 10_000));
        int s = Math.max(1, Math.min(size, 100));
        String query = q == null || q.isBlank() ? null : q.trim();
        String r = role == null || role.isBlank() ? null : role;
        return PageResult.of(admin.searchUsers(query, r, teamId, (p - 1) * s, s), p, s, admin.countUsers(query, r, teamId));
    }

    /**
     * Changing a user's role or deactivating them revokes their refresh tokens, so the change takes
     * effect within one access-token lifetime (15 min) on every device.
     */
    @Transactional
    public AdminUserView updateUser(CurrentUser caller, long userId, UpdateUserRequest req) {
        AdminUserView target = admin.findUser(userId);
        if (target == null) {
            throw ApiException.notFound("User");
        }
        boolean losesAdmin = Role.ADMIN.name().equals(target.getRole())
                && (!Role.ADMIN.name().equals(req.role()) || !req.active());
        if (losesAdmin && caller.id() == userId) {
            throw ApiException.badRequest("You can't remove your own admin access");
        }
        if (losesAdmin && admin.countActiveAdmins() <= 1) {
            throw ApiException.badRequest("At least one active admin is required");
        }
        if (req.teamId() != null && admin.findTeam(req.teamId()) == null) {
            throw ApiException.badRequest("Team does not exist");
        }
        admin.updateUser(userId, req.role(), req.teamId(), req.active());
        if (!Objects.equals(target.getRole(), req.role()) || !req.active()) {
            tokens.revokeAll(userId);
        }
        return admin.findUser(userId);
    }

    // ---------- teams ----------

    public List<Team> teams(CurrentUser caller) {
        return admin.listTeams(scope(caller));
    }

    @Transactional
    public Team createTeam(TeamRequest req) {
        Team t = new Team();
        apply(t, req);
        try {
            admin.insertTeam(t);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("A team with that name already exists");
        }
        return admin.findTeam(t.getId());
    }

    @Transactional
    public Team updateTeam(long teamId, TeamRequest req) {
        Team t = admin.findTeam(teamId);
        if (t == null) {
            throw ApiException.notFound("Team");
        }
        apply(t, req);
        try {
            admin.updateTeam(t);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("A team with that name already exists");
        }
        return admin.findTeam(teamId);
    }

    @Transactional
    public void deleteTeam(long teamId) {
        if (admin.deleteTeam(teamId) == 0) {
            throw ApiException.notFound("Team");
        }
    }

    private void apply(Team t, TeamRequest req) {
        t.setName(req.name().trim());
        if (req.managerId() != null) {
            User m = users.findById(req.managerId());
            if (m == null || !Role.valueOf(m.getRole()).canManage()) {
                throw ApiException.badRequest("Team manager must be a user with the MANAGER or ADMIN role");
            }
        }
        t.setManagerId(req.managerId());
    }
}
