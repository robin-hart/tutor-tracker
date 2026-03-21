package com.tutortimetracker.api.model;

/**
 * Student profile item rendered in the student management view.
 *
 * @param id stable student identifier
 * @param name student full name
 * @param notes progress notes for tutoring context
 * @param groupName student group within the project
 */
public record StudentProfile(
        String id,
        String name,
        String notes,
        String groupName
) {
}
