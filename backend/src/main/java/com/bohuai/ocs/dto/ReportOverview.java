package com.bohuai.ocs.dto;

import com.bohuai.ocs.model.CourseStat;
import com.bohuai.ocs.model.TeamStat;

import java.util.List;

public record ReportOverview(long learners, int assignments, int completed, int overdue, double completionRate,
                             List<CourseStat> courses, List<TeamStat> teams) {
}
