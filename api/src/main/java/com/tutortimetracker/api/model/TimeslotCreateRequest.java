package com.tutortimetracker.api.model;

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
        @NotBlank String title,
        String description,
        @Min(15) int durationMinutes,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime
) {
}
