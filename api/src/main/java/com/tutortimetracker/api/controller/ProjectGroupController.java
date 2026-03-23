package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.ProjectGroupCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupSummary;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages student groups within projects.
 *
 * <p>Handles listing, creating, and deleting groups. Group deletion automatically reassigns
 * students to the Ungrouped default group.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Project Groups", description = "Group management within projects")
public class ProjectGroupController {

  private final TutorDataService tutorDataService;

  public ProjectGroupController(TutorDataService tutorDataService) {
    this.tutorDataService = tutorDataService;
  }

  @Operation(
      summary = "List project groups",
      description = "Returns project groups with student counts.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Groups loaded",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ProjectGroupSummary.class)))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping("/projects/{projectId}/groups")
  public List<ProjectGroupSummary> getProjectGroups(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId) {
    return tutorDataService.getProjectGroups(projectId);
  }

  @Operation(
      summary = "Create project group",
      description = "Creates a new group in the selected project.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Group created",
            content = @Content(schema = @Schema(implementation = ProjectGroupSummary.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping("/projects/{projectId}/groups")
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectGroupSummary createProjectGroup(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Group creation payload",
              required = true,
              content =
                  @Content(examples = @ExampleObject(value = "{\n  \"name\": \"Group A\"\n}")))
          @Valid
          @RequestBody
          ProjectGroupCreateRequest request) {
    return tutorDataService.createProjectGroup(projectId, request);
  }

  @Operation(
      summary = "Delete project group",
      description = "Deletes one group and moves students to Ungrouped.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Group deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "Illegal group operation",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @DeleteMapping("/projects/{projectId}/groups/{groupName}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProjectGroup(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @Parameter(description = "Group name", example = "Group A") @PathVariable String groupName) {
    tutorDataService.deleteProjectGroup(projectId, groupName);
  }
}
