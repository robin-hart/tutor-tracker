package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "UTC timestamp of the error event", example = "2026-03-21T15:12:40Z")
        Instant timestamp,
    @Schema(description = "HTTP status code", example = "404") int status,
    @Schema(description = "HTTP status reason phrase", example = "Not Found") String error,
    @Schema(
            description = "Human-readable error details",
            example = "Project not found: unknown-project")
        String message,
    @Schema(
            description = "Request path that produced the error",
            example = "/api/projects/unknown-project")
        String path) {}
