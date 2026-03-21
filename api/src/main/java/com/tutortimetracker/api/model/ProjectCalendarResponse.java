package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Calendar payload for a specific project.
 *
 * @param projectId project identifier
 * @param projectName display title
 * @param totalHours total hours for the project
 * @param monthHours hours tracked this month
 * @param todaySlots list of slots scheduled for current day
 * @param monthSlots list of slots for the active month
 * @param allSlots list of all slots for the project across all months
 */
public record ProjectCalendarResponse(
        @Schema(description = "Project identifier", example = "math-grade-10")
        String projectId,
        @Schema(description = "Project display name", example = "Math Grade 10")
        String projectName,
        @Schema(description = "All-time tracked hours", example = "42.5")
        double totalHours,
        @Schema(description = "Current month tracked hours", example = "6.5")
        double monthHours,
        @Schema(description = "Timeslots scheduled for the current day")
        List<TodaySlot> todaySlots,
        @Schema(description = "Timeslots in the requested month")
        List<CalendarSlot> monthSlots,
        @Schema(description = "All project timeslots across months")
        List<CalendarSlot> allSlots
) {
}
