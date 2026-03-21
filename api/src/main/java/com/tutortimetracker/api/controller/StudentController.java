package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.StudentGroupUpdateRequest;
import com.tutortimetracker.api.model.StudentNotesUpdateRequest;
import com.tutortimetracker.api.model.StudentProfile;
import com.tutortimetracker.api.service.TutorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Students", description = "Student update and delete operations")
public class StudentController {

  private final TutorDataService tutorDataService;

  public StudentController(TutorDataService tutorDataService) {
    this.tutorDataService = tutorDataService;
  }

  @Operation(summary = "Update student notes", description = "Updates notes for one student.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Student updated",
            content = @Content(schema = @Schema(implementation = StudentProfile.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PatchMapping("/students/{studentId}/notes")
  public StudentProfile updateStudentNotes(
      @Parameter(description = "Student key", example = "student-123") @PathVariable
          String studentId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Notes update payload",
              required = true,
              content =
                  @Content(
                      examples =
                          @ExampleObject(
                              value =
                                  "{\n"
                                      + "  \"notes\": \"Improved in fractions and equations.\"\n"
                                      + "}")))
          @Valid
          @RequestBody
          StudentNotesUpdateRequest request) {
    return tutorDataService.updateStudentNotes(studentId, request);
  }

  @Operation(
      summary = "Update student group",
      description = "Reassigns a student to another group.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Student updated",
            content = @Content(schema = @Schema(implementation = StudentProfile.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Student or project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @PatchMapping("/students/{studentId}/group")
  public StudentProfile updateStudentGroup(
      @Parameter(description = "Student key", example = "student-123") @PathVariable
          String studentId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Group update payload",
              required = true,
              content =
                  @Content(examples = @ExampleObject(value = "{\n  \"groupName\": \"Group B\"\n}")))
          @Valid
          @RequestBody
          StudentGroupUpdateRequest request) {
    return tutorDataService.updateStudentGroup(studentId, request);
  }

  @Operation(summary = "Delete student", description = "Deletes a student profile.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Student deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
      })
  @DeleteMapping("/students/{studentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStudent(
      @Parameter(description = "Student key", example = "student-123") @PathVariable
          String studentId) {
    tutorDataService.deleteStudent(studentId);
  }
}
