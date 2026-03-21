package com.tutortimetracker.api.model;

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
        String id,
        String projectId,
        String title,
        String description,
        int durationMinutes,
        LocalDate date,
        LocalTime startTime
) {
}
