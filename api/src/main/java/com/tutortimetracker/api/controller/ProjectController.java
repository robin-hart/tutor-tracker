package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.ProjectCalendarResponse;
import com.tutortimetracker.api.model.ProjectCreateRequest;
import com.tutortimetracker.api.model.ProjectSummary;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Projects", description = "Project overview and calendar endpoints")
public class ProjectController {

    private final TutorDataService tutorDataService;

    public ProjectController(TutorDataService tutorDataService) {
        this.tutorDataService = tutorDataService;
    }

    @Operation(summary = "List projects", description = "Returns all projects used by the dashboard overview.")
    @ApiResponse(responseCode = "200", description = "Projects loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectSummary.class))))
    @GetMapping("/projects")
    public List<ProjectSummary> getProjects() {
        return tutorDataService.getProjects();
    }

    @Operation(summary = "Create project", description = "Creates a new project with initial metrics and default group setup.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created", content = @Content(schema = @Schema(implementation = ProjectSummary.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectSummary createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return tutorDataService.createProject(request);
    }

    @Operation(summary = "Delete project", description = "Deletes a project and all dependent resources.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Project deleted"),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId) {
        tutorDataService.deleteProject(projectId);
    }

    @Operation(summary = "Get project calendar", description = "Returns calendar data and today's slots for a project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calendar loaded", content = @Content(schema = @Schema(implementation = ProjectCalendarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/projects/{projectId}/calendar")
    public ProjectCalendarResponse getProjectCalendar(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Month key in yyyy-MM format", example = "2026-03") @RequestParam(required = false) String month
    ) {
        return tutorDataService.getProjectCalendar(projectId, month);
    }
}
