package com.bohuai.ocs.mapper;

import com.bohuai.ocs.model.AdminUserView;
import com.bohuai.ocs.model.AssignmentRow;
import com.bohuai.ocs.model.CourseStat;
import com.bohuai.ocs.model.Team;
import com.bohuai.ocs.model.TeamStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin, team, assignment and reporting queries. Methods taking {@code managerId} restrict results
 * to members of teams managed by that user; {@code null} means organisation-wide (admins).
 */
@Mapper
public interface AdminMapper {

    // ---------- users ----------
    List<AdminUserView> searchUsers(@Param("q") String q, @Param("role") String role, @Param("teamId") Long teamId,
                                    @Param("offset") int offset, @Param("limit") int limit);

    long countUsers(@Param("q") String q, @Param("role") String role, @Param("teamId") Long teamId);

    AdminUserView findUser(@Param("id") long id);

    int updateUser(@Param("id") long id, @Param("role") String role, @Param("teamId") Long teamId,
                   @Param("active") boolean active);

    long countActiveAdmins();

    // ---------- teams ----------
    List<Team> listTeams(@Param("managerId") Long managerId);

    Team findTeam(@Param("id") long id);

    int insertTeam(Team team);

    int updateTeam(Team team);

    int deleteTeam(@Param("id") long id);

    /** Ids of active users in a team (scoped to the manager when given). */
    List<Long> teamMemberIds(@Param("teamId") long teamId, @Param("managerId") Long managerId);

    /** Of the given user ids, those inside the manager's scope (all, for admins). */
    List<Long> usersInScope(@Param("userIds") List<Long> userIds, @Param("managerId") Long managerId);

    // ---------- assignments ----------
    /** The existing assignment (id and due date only), row-locked, or null. */
    AssignmentRow lockAssignment(@Param("userId") long userId, @Param("courseId") long courseId);

    int insertAssignment(@Param("userId") long userId, @Param("courseId") long courseId,
                         @Param("assignedBy") long assignedBy, @Param("dueDate") LocalDate dueDate);

    int updateDueDate(@Param("id") long id, @Param("dueDate") LocalDate dueDate);

    int enrollIfMissing(@Param("userId") long userId, @Param("courseId") long courseId);

    AssignmentRow findAssignment(@Param("id") long id, @Param("managerId") Long managerId);

    int deleteAssignment(@Param("id") long id);

    List<AssignmentRow> listAssignments(@Param("managerId") Long managerId, @Param("courseId") Long courseId,
                                        @Param("teamId") Long teamId, @Param("userId") Long userId,
                                        @Param("limit") int limit);

    // ---------- reports ----------
    List<CourseStat> courseStats(@Param("managerId") Long managerId);

    List<TeamStat> teamStats(@Param("managerId") Long managerId);

    long countLearnersInScope(@Param("managerId") Long managerId);
}
