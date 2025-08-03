package com.bohuai.ocs.dto;

import java.util.List;

/** What the current caller can do with a course (null fields for anonymous visitors). */
public record ViewerState(boolean signedIn, boolean enrolled, String enrollmentStatus, boolean canEdit,
                          List<Long> completedChapterIds, String certificateCode) {

    public static ViewerState anonymous() {
        return new ViewerState(false, false, null, false, List.of(), null);
    }
}
