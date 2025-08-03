package com.bohuai.ocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/** An answer option. {@code correct} is null in learner-facing responses. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long questionId;
    private Integer position;
    private String text;
    private Boolean correct;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
