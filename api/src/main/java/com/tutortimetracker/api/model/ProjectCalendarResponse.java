package com.tutortimetracker.api.model;

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
 */
public record ProjectCalendarResponse(
        String projectId,
        String projectName,
        double totalHours,
        double monthHours,
        List<TodaySlot> todaySlots,
        List<CalendarSlot> monthSlots
) {
}
