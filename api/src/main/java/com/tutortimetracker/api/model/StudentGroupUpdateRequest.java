package com.tutortimetracker.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for assigning a student to a project group.
 *
 * @param groupName target group name
 */
public record StudentGroupUpdateRequest(
        @NotBlank String groupName
) {
}
