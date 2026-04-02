package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a project.
 *
 * @param name display project name
 * @param category project category tag
 * @param institution institute or workplace name
 * @param targetMonthHours monthly target working time
 * @param completionPercent progress percentage from 0 to 100
 */
public record ProjectCreateRequest(
    @Schema(description = "Display name of the project", example = "Math Grade 10") @NotBlank
        String name,
    @Schema(description = "Project category label", example = "Mathematics") @NotBlank
        String category,
    @Schema(description = "Institute or workplace", example = "University of Applied Sciences")
        @NotBlank
        String institution,
    @Schema(description = "Monthly target working time", example = "12.5") @NotNull @Min(0)
        Double targetMonthHours,
    @Schema(description = "Completion percent from 0 to 100", example = "35") @Min(0) @Max(100)
        int completionPercent) {}
