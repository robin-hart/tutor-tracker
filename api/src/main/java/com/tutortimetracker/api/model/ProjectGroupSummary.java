package com.tutortimetracker.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Project group item for student grouping UI.
 *
 * @param name group name
 * @param studentCount number of students in this group
 */
public record ProjectGroupSummary(
        @Schema(description = "Group name", example = "Group A")
        String name,
        @Schema(description = "Number of students assigned to this group", example = "5")
        int studentCount
) {
}
