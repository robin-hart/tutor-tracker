package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.TimeslotCreateRequest;
import com.tutortimetracker.api.model.TimeslotResponse;
import com.tutortimetracker.api.service.TutorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Timeslots", description = "Timeslot CRUD endpoints")
public class TimeslotController {

  private final TutorDataService tutorDataService;

  public TimeslotController(TutorDataService tutorDataService) {
    this.tutorDataService = tutorDataService;
  }

  @Operation(
      summary = "Create global timeslot",
      description = "Legacy endpoint for creating a timeslot outside project route scope.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Timeslot created",
            content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping("/timeslots")
  public TimeslotResponse createTimeslot(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Timeslot payload",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  "{\n"
                                      + "  \"title\": \"Integration Workshop\",\n"
                                      + "  \"description\": \"Focused revision session\",\n"
                                      + "  \"durationMinutes\": 90,\n"
                                      + "  \"date\": \"2026-03-20\",\n"
                                      + "  \"startTime\": \"14:00\"\n"
                                      + "}")))
          @Valid
          @RequestBody
          TimeslotCreateRequest request) {
    return tutorDataService.createTimeslot(request);
  }

  @Operation(
      summary = "List project timeslots",
      description = "Returns project timeslots, optionally filtered by month.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Timeslots loaded",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = TimeslotResponse.class)))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping("/projects/{projectId}/timeslots")
  public List<TimeslotResponse> getProjectTimeslots(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(description = "Month key in yyyy-MM format", example = "2026-03")
          @RequestParam(required = false)
          String month) {
    return tutorDataService.getProjectTimeslots(projectId, month);
  }

  @Operation(
      summary = "Create project timeslot",
      description = "Creates a timeslot in project calendar context.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Timeslot created",
            content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping("/projects/{projectId}/timeslots")
  @ResponseStatus(HttpStatus.CREATED)
  public TimeslotResponse createProjectTimeslot(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Valid @RequestBody TimeslotCreateRequest request) {
    return tutorDataService.createProjectTimeslot(projectId, request);
  }

  @Operation(
      summary = "Get project timeslot",
      description = "Returns one timeslot by id in project context.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Timeslot loaded",
            content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project or timeslot not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping("/projects/{projectId}/timeslots/{timeslotId}")
  public TimeslotResponse getProjectTimeslot(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(
              description = "Timeslot identifier",
              example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14")
          @PathVariable
          String timeslotId) {
    return tutorDataService.getProjectTimeslot(projectId, timeslotId);
  }

  @Operation(
      summary = "Update project timeslot",
      description = "Updates a single project timeslot.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Timeslot updated",
            content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project or timeslot not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PutMapping("/projects/{projectId}/timeslots/{timeslotId}")
  public TimeslotResponse updateProjectTimeslot(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(
              description = "Timeslot identifier",
              example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14")
          @PathVariable
          String timeslotId,
      @Valid @RequestBody TimeslotCreateRequest request) {
    return tutorDataService.updateProjectTimeslot(projectId, timeslotId, request);
  }

  @Operation(
      summary = "Delete project timeslot",
      description = "Deletes one timeslot from a project calendar.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Timeslot deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "Project or timeslot not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @DeleteMapping("/projects/{projectId}/timeslots/{timeslotId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProjectTimeslot(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(
              description = "Timeslot identifier",
              example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14")
          @PathVariable
          String timeslotId) {
    tutorDataService.deleteProjectTimeslot(projectId, timeslotId);
  }
}
