package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API response returned after creating a timeslot.
 *
 * @param id generated timeslot id
 * @param projectId owning project id
 * @param title title used for display
 * @param description detailed notes
 * @param durationMinutes duration value
 * @param date scheduled date
 * @param startTime scheduled start time
 */
public record TimeslotResponse(
    @Schema(description = "Timeslot identifier", example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14")
        String id,
    @Schema(description = "Owning project identifier", example = "math-grade-10") String projectId,
    @Schema(description = "Timeslot title", example = "Integration Workshop") String title,
    @Schema(description = "Timeslot description", example = "Focused revision session")
        String description,
    @Schema(description = "Duration in minutes", example = "90") int durationMinutes,
    @Schema(description = "Scheduled date", example = "2026-03-20") LocalDate date,
    @Schema(description = "Scheduled start time", example = "14:00") LocalTime startTime) {}
