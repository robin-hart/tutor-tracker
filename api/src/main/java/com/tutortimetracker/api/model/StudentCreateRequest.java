package com.tutortimetracker.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a student under a project.
 *
 * @param name full student name
 * @param notes optional initial progress notes
 * @param groupName optional initial student group
 */
public record StudentCreateRequest(
        @NotBlank String name,
        String notes,
        String groupName
) {
}
