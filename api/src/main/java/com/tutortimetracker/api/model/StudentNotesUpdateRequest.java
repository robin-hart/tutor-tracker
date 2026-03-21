package com.tutortimetracker.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating a student's notes.
 *
 * @param notes new progress notes text
 */
public record StudentNotesUpdateRequest(@NotBlank String notes) {
}
