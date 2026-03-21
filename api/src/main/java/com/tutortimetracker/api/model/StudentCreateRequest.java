package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a student under a project.
 *
 * @param name full student name
 * @param notes optional initial progress notes
 * @param groupName optional initial student group
 */
public record StudentCreateRequest(
        @Schema(description = "Student full name", example = "Alex Thompson")
        @NotBlank String name,
        @Schema(description = "Optional tutoring notes", example = "Needs additional practice on quadratics.")
        String notes,
        @Schema(description = "Optional initial group name", example = "Group A")
        String groupName
) {
}
