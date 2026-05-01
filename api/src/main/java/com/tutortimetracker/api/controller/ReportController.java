package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.ReportPdfExportRequest;
import com.tutortimetracker.api.model.ReportRow;
import com.tutortimetracker.api.service.ProjectReportPdfService;
import com.tutortimetracker.api.service.TutorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates and retrieves tutoring session reports across projects.
 *
 * <p>Provides global report aggregation, project-scoped reporting, and monthly report generation
 * for analytics and time tracking.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Reports", description = "Project and global reporting endpoints")
public class ReportController {

  private final TutorDataService tutorDataService;
  private final ProjectReportPdfService projectReportPdfService;

  public ReportController(
      TutorDataService tutorDataService, ProjectReportPdfService projectReportPdfService) {
    this.tutorDataService = tutorDataService;
    this.projectReportPdfService = projectReportPdfService;
  }

  @Operation(summary = "List reports", description = "Returns report rows across all projects.")
  @ApiResponse(
      responseCode = "200",
      description = "Reports loaded",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReportRow.class))))
  @GetMapping("/reports")
  public List<ReportRow> getReports() {
    return tutorDataService.getReports();
  }

  @Operation(summary = "List project reports", description = "Returns report rows for one project.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Project reports loaded",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = ReportRow.class)))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping("/projects/{projectId}/reports")
  public List<ReportRow> getProjectReports(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId) {
    return tutorDataService.getProjectReports(projectId);
  }

  @Operation(
      summary = "Generate monthly report",
      description = "Generates or refreshes one monthly report for a project.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Report generated",
            content = @Content(schema = @Schema(implementation = ReportRow.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid month or input",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping("/projects/{projectId}/reports/generate")
  public ReportRow generateProjectReport(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(description = "Month key in yyyy-MM format", example = "2026-03") @RequestParam
          String month) {
    return tutorDataService.generateProjectMonthlyReport(projectId, month);
  }

  @Operation(
      summary = "Export monthly project report PDF",
      description =
          "Builds a LaTeX report from project timeslots in the selected month and returns a PDF"
              + " download.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "PDF generated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid month format",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "LaTeX compiler unavailable or generation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping(value = "/projects/{projectId}/reports/export/pdf", produces = "application/pdf")
  public ResponseEntity<byte[]> exportProjectReportPdf(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(description = "Month key in yyyy-MM format", example = "2026-03") @RequestParam
          String month) {
    byte[] pdf = projectReportPdfService.exportProjectMonthPdf(projectId, month, null, null);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(projectId + "-" + month + "-report.pdf").build());

    return ResponseEntity.ok().headers(headers).body(pdf);
  }

  @Operation(
      summary = "Export monthly project report PDF (with tutor details)",
      description =
          "Builds a LaTeX report from project timeslots and optional tutor name/signature."
              + " Returns a PDF download.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "PDF generated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid month or input",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "LaTeX compiler unavailable or generation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping(value = "/projects/{projectId}/reports/export/pdf", produces = "application/pdf")
  public ResponseEntity<byte[]> exportProjectReportPdf(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Valid @RequestBody ReportPdfExportRequest request) {
    byte[] pdf =
        projectReportPdfService.exportProjectMonthPdf(
            projectId, request.month(), request.tutorName(), request.signatureDataUrl());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename(projectId + "-" + request.month() + "-report.pdf")
            .build());

    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
