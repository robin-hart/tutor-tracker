package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for assigning a student to a project group.
 *
 * @param groupName target group name
 */
public record StudentGroupUpdateRequest(
    @Schema(description = "Target group name", example = "Group B") @NotBlank String groupName) {}
