package com.bohuai.ocs.service;

import com.bohuai.ocs.mapper.ActivityMapper;
import com.bohuai.ocs.model.Activity;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityService {

    public static final String ENROLL = "ENROLL";
    public static final String UNENROLL = "UNENROLL";
    public static final String VIEW_CHAPTER = "VIEW_CHAPTER";
    public static final String COMPLETE_CHAPTER = "COMPLETE_CHAPTER";
    public static final String COMPLETE_COURSE = "COMPLETE_COURSE";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String QUIZ_PASSED = "QUIZ_PASSED";
    public static final String QUIZ_FAILED = "QUIZ_FAILED";

    private final ActivityMapper mapper;
    private final MeterRegistry meters;

    public ActivityService(ActivityMapper mapper, MeterRegistry meters) {
        this.mapper = mapper;
        this.meters = meters;
    }

    /** Persists the event and counts it as the {@code ocs.learning.events{action=...}} metric. */
    public void log(long userId, Long courseId, Long chapterId, String action) {
        mapper.insert(userId, courseId, chapterId, action);
        meters.counter("ocs.learning.events", "action", action).increment();
    }

    public List<Activity> recent(long userId, int limit) {
        return mapper.recentForUser(userId, Math.max(1, Math.min(limit, 100)));
    }
}
