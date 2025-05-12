package com.bohuai.ocs.service;

import com.bohuai.ocs.mapper.ActivityMapper;
import com.bohuai.ocs.model.Activity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityService {

    public static final String ENROLL = "ENROLL";
    public static final String UNENROLL = "UNENROLL";
    public static final String VIEW_CHAPTER = "VIEW_CHAPTER";
    public static final String COMPLETE_CHAPTER = "COMPLETE_CHAPTER";
    public static final String COMPLETE_COURSE = "COMPLETE_COURSE";

    private final ActivityMapper mapper;

    public ActivityService(ActivityMapper mapper) {
        this.mapper = mapper;
    }

    public void log(long userId, Long courseId, Long chapterId, String action) {
        mapper.insert(userId, courseId, chapterId, action);
    }

    public List<Activity> recent(long userId, int limit) {
        return mapper.recentForUser(userId, Math.max(1, Math.min(limit, 100)));
    }
}
