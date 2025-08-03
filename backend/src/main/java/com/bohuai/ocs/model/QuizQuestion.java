package com.bohuai.ocs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** A single-answer multiple-choice question attached to a chapter. */
public class QuizQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chapterId;
    private Integer position;
    private String prompt;
    private String explanation;
    private List<QuizOption> options = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<QuizOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuizOption> options) {
        this.options = options;
    }
}
