package com.tutortimetracker.api.model;

import java.time.Instant;

/**
 * Standard API error payload.
 *
 * @param timestamp event time in UTC
 * @param status HTTP status code
 * @param error short error name
 * @param message human-readable details
 * @param path request path
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
