package com.tutortimetracker.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a project.
 *
 * @param name display project name
 * @param category project category tag
 * @param totalHours historical tracked hours
 * @param monthHours tracked hours in current month
 * @param completionPercent progress percentage from 0 to 100
 */
public record ProjectCreateRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Min(0) Double totalHours,
        @NotNull @Min(0) Double monthHours,
        @Min(0) @Max(100) int completionPercent
) {
}
