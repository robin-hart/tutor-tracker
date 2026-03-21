package com.tutortimetracker.api.model;

/**
 * Project group item for student grouping UI.
 *
 * @param name group name
 * @param studentCount number of students in this group
 */
public record ProjectGroupSummary(
        String name,
        int studentCount
) {
}
