package com.tutortimetracker.api.model;

/**
 * Summary metrics shown on the projects dashboard.
 *
 * @param id stable project identifier
 * @param name display name for the project
 * @param category project category chip label
 * @param totalHours all-time tracked hours
 * @param monthHours tracked hours in current month
 * @param completionPercent completion ratio from 0-100
 */
public record ProjectSummary(
        String id,
        String name,
        String category,
        double totalHours,
        double monthHours,
        int completionPercent
) {
}
