package com.bohuai.ocs.model;

import java.io.Serializable;
import java.time.Instant;

/** A learner's enrollment joined with course info and progress counts (dashboard row). */
public class EnrollmentView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long courseId;
    private String slug;
    private String title;
    private String summary;
    private String category;
    private String level;
    private String instructorName;
    private String status;
    private Instant enrolledAt;
    private Instant completedAt;
    private Integer totalChapters;
    private Integer completedChapters;
    private Long nextChapterId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(Integer totalChapters) {
        this.totalChapters = totalChapters;
    }

    public Integer getCompletedChapters() {
        return completedChapters;
    }

    public void setCompletedChapters(Integer completedChapters) {
        this.completedChapters = completedChapters;
    }

    public Long getNextChapterId() {
        return nextChapterId;
    }

    public void setNextChapterId(Long nextChapterId) {
        this.nextChapterId = nextChapterId;
    }
}
