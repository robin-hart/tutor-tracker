package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.ReportRow;
import com.tutortimetracker.api.service.TutorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Reports", description = "Project and global reporting endpoints")
public class ReportController {

  private final TutorDataService tutorDataService;

  public ReportController(TutorDataService tutorDataService) {
    this.tutorDataService = tutorDataService;
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
}
