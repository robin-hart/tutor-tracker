package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for creating or updating a timeslot.
 *
 * @param title session title
 * @param description optional text description
 * @param durationMinutes duration in minutes
 * @param date scheduled date
 * @param startTime scheduled start time
 */
public record TimeslotCreateRequest(
    @Schema(description = "Session title", example = "Integration Workshop") @NotBlank String title,
    @Schema(description = "Optional details", example = "Focused revision session")
        String description,
    @Schema(description = "Session duration in minutes (minimum 15)", example = "90") @Min(15)
        int durationMinutes,
    @Schema(description = "Session date", example = "2026-03-20") @NotNull LocalDate date,
    @Schema(description = "Session start time", example = "14:00") @NotNull LocalTime startTime) {}
