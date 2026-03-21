package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Student profile item rendered in the student management view.
 *
 * @param id stable student identifier
 * @param name student full name
 * @param notes progress notes for tutoring context
 * @param groupName student group within the project
 */
public record StudentProfile(
    @Schema(description = "Stable student identifier", example = "student-123") String id,
    @Schema(description = "Student full name", example = "Alex Thompson") String name,
    @Schema(description = "Tutor notes", example = "Needs additional practice on quadratics.")
        String notes,
    @Schema(description = "Assigned project group", example = "Group A") String groupName) {}
