package com.tutortimetracker.api.model;

/**
 * A single report row for monthly export tables.
 *
 * @param projectId owning project identifier
 * @param month period label
 * @param projectName project title
 * @param totalHours tracked hours
 * @param sessions total sessions count
 * @param grossAmount calculated billing amount
 */
public record ReportRow(
         String projectId,
        String month,
        String projectName,
        double totalHours,
        int sessions,
        double grossAmount
) {
}
