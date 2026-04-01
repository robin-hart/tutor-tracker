package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.StudentCreateRequest;
import com.tutortimetracker.api.model.StudentProfile;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages student enrollment and listing within project scope.
 *
 * <p>Handles listing all students in a project and creating new student profiles for project
 * participation.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Project Students", description = "Student listing and creation within project scope")
public class ProjectStudentController {

  private final TutorDataService tutorDataService;

  public ProjectStudentController(TutorDataService tutorDataService) {
    this.tutorDataService = tutorDataService;
  }

  @Operation(
      summary = "List project students",
      description = "Returns all students assigned to the selected project.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Students loaded",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = StudentProfile.class)))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @GetMapping("/projects/{projectId}/students")
  public List<StudentProfile> getProjectStudents(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId) {
    return tutorDataService.getProjectStudents(projectId);
  }

  @Operation(
      summary = "Create project student",
      description = "Creates a student profile in a project.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Student created",
            content = @Content(schema = @Schema(implementation = StudentProfile.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PostMapping("/projects/{projectId}/students")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentProfile createProjectStudent(
      @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable
          String projectId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Student creation payload",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  """
                                  {
                                    "name": "Alex Thompson",
                                    "notes": "Needs extra practice on quadratics.",
                                    "groupName": "Group A"
                                  }""")))
          @Valid
          @RequestBody
          StudentCreateRequest request) {
    return tutorDataService.createProjectStudent(projectId, request);
  }
}
