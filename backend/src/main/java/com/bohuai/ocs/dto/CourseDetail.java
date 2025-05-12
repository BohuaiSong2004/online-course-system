package com.bohuai.ocs.dto;

import com.bohuai.ocs.model.Chapter;
import com.bohuai.ocs.model.Course;

import java.util.List;

public record CourseDetail(Course course, List<Chapter> chapters, long enrollmentCount, ViewerState viewer) {
}
