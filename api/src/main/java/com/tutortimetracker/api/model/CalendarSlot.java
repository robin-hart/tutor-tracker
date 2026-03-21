package com.tutortimetracker.api.model;

/**
 * Calendar slot data for month-level rendering.
 *
 * @param id timeslot id
 * @param title session title
 * @param date ISO date string
 * @param startTime slot start time
 * @param durationMinutes duration in minutes
 */
public record CalendarSlot(
    String id, String title, String date, String startTime, int durationMinutes) {}
