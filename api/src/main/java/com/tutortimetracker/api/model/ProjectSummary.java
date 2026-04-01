package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Summary metrics shown on the projects dashboard.
 *
 * @param id stable project identifier
 * @param name display name for the project
 * @param category project category chip label
 * @param totalHours all-time tracked hours
 * @param monthHours tracked hours in current month
 * @param completionPercent completion ratio from 0-100
 * @param createdAt project creation date
 */
public record ProjectSummary(
    @Schema(description = "Stable project identifier", example = "math-grade-10") String id,
    @Schema(description = "Project display name", example = "Math Grade 10") String name,
    @Schema(description = "Project category", example = "Mathematics") String category,
    @Schema(description = "All-time tracked hours", example = "42.5") double totalHours,
    @Schema(description = "Current month tracked hours", example = "6.5") double monthHours,
    @Schema(description = "Completion percentage", example = "35") int completionPercent,
    @Schema(description = "Project creation date", example = "2026-03-01") LocalDate createdAt) {}
