package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "Project identifier", example = "math-grade-10") String projectId,
    @Schema(description = "Reporting month", example = "2026-03") String month,
    @Schema(description = "Project display name", example = "Math Grade 10") String projectName,
    @Schema(description = "Total tracked hours in month", example = "12.5") double totalHours,
    @Schema(description = "Session count in month", example = "8") int sessions,
    @Schema(description = "Gross amount calculated for the month", example = "750.0")
        double grossAmount) {}
