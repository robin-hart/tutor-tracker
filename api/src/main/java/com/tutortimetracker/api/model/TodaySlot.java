package com.tutortimetracker.api.model;

/**
 * A single time slot displayed in today's schedule cards.
 *
 * @param title slot title
 * @param time human-readable slot time
 * @param description slot details
 */
public record TodaySlot(String title, String time, String description) {}
