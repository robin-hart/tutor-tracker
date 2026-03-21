package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating a student's notes.
 *
 * @param notes new progress notes text
 */
public record StudentNotesUpdateRequest(
	@Schema(description = "Updated notes text", example = "Improved in fractions and linear equations.")
	@NotBlank String notes
) {
}
