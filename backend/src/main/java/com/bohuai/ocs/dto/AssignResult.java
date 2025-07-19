package com.bohuai.ocs.dto;

/**
 * @param created   new assignments
 * @param updated   existing assignments whose due date changed
 * @param unchanged already assigned with the same due date
 * @param skipped   requested users outside the caller's scope or inactive
 */
public record AssignResult(int created, int updated, int unchanged, int skipped) {
}
