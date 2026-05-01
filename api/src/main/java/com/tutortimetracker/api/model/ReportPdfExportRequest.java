package com.tutortimetracker.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for exporting a PDF report with optional tutor details. */
public record ReportPdfExportRequest(
    @NotBlank(message = "is required") String month,
    @Size(max = 120, message = "must be 120 characters or fewer") String tutorName,
    @Size(max = 600000, message = "is too large") String signatureDataUrl) {}
