package com.tutortimetracker.api.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a project group.
 *
 * @param name group name
 */
public record ProjectGroupCreateRequest(@NotBlank String name) {}
