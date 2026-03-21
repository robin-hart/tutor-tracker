package com.tutortimetracker.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a report row.
 *
 * @param month report period label
 * @param projectName project display name
 * @param totalHours total tracked hours in period
 * @param sessions number of sessions in period
 * @param grossAmount computed billing amount
 */
public record ReportCreateRequest(
    @NotBlank String month,
    @NotBlank String projectName,
    @NotNull @Min(0) Double totalHours,
    @Min(0) int sessions,
    @NotNull @Min(0) Double grossAmount) {}
