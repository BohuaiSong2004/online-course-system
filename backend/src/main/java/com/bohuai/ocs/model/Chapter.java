package com.bohuai.ocs.model;

import java.io.Serializable;
import java.time.Instant;

/** An ordered lesson inside a course. content is Markdown. */
public class Chapter implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long courseId;
    private String title;
    private String content;
    private Integer position;
    private Integer durationMin;
    private Boolean preview;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public Boolean getPreview() {
        return preview;
    }

    public void setPreview(Boolean preview) {
        this.preview = preview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
