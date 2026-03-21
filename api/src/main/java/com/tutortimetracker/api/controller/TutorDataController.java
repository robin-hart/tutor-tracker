package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.model.ProjectCalendarResponse;
import com.tutortimetracker.api.model.ProjectGroupCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupSummary;
import com.tutortimetracker.api.model.ProjectCreateRequest;
import com.tutortimetracker.api.model.ProjectSummary;
import com.tutortimetracker.api.model.ReportRow;
import com.tutortimetracker.api.model.StudentCreateRequest;
import com.tutortimetracker.api.model.StudentGroupUpdateRequest;
import com.tutortimetracker.api.model.StudentProfile;
import com.tutortimetracker.api.model.StudentNotesUpdateRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Main REST API controller for TutorTimeTracker frontend consumption.
 *
 * <p>This controller provides a comprehensive REST API for managing tutoring projects,
 * student records, timeslots, and performance reports. The API is organized around two main
 * resource hierarchies:</p>
 *
 * <h2>Project-Scoped Resources</h2>
 * <ul>
 *   <li>Projects: Dashboard cards with hour metrics and completion tracking</li>
 *   <li>Students: Tutoring students assigned to projects with notes and group management</li>
 *   <li>Groups: Student grouping for organizational purposes (e.g., "Honors", "Remedial")</li>
 *   <li>Timeslots: Individual tutoring sessions with duration and scheduling</li>
 *   <li>Reports: Monthly aggregated metrics (hours, sessions, revenue)</li>
 *   <li>Calendar: Combined month/day view of timeslots with project metrics</li>
 * </ul>
 *
 * <h2>Global Resources</h2>
 * <ul>
 *   <li>Timeslots: Global timeslot creation (legacy support)</li>
 *   <li>Reports: Cross-project report aggregation</li>
 * </ul>
 *
 * <p>All endpoints support JSON request/response payloads with validation.
 * CORS is enabled for frontend at http://localhost:5173.</p>
 *
 * @see TutorDataService
 * @author TutorTimeTracker Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
@Tag(name = "Tutor Data", description = "Project, student, group, timeslot, calendar and reporting endpoints")
public class TutorDataController {

    private final TutorDataService tutorDataService;

    /**
     * @param tutorDataService service providing dashboard and project data
     */
    public TutorDataController(TutorDataService tutorDataService) {
        this.tutorDataService = tutorDataService;
    }

    /**
     * Returns all dashboard project cards.
     *
     * @return project summaries
     */
    @Operation(summary = "List projects", description = "Returns all projects used by the dashboard overview.")
    @ApiResponse(responseCode = "200", description = "Projects loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectSummary.class))))
    @GetMapping("/projects")
    public List<ProjectSummary> getProjects() {
        return tutorDataService.getProjects();
    }

    /**
     * Creates a new project card.
     *
     * @param request request body
     * @return created project summary
     */
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

    /**
     * Deletes a project and all associated data (students, timeslots, groups, reports).
     *
     * @param projectId route project id
     */
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

    /**
     * Returns project calendar metadata and today's slots.
     *
     * @param projectId route project id
     * @return calendar payload
     */
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

    /**
     * Returns student cards for a project.
     *
     * @param projectId route project id
     * @return students list
     */
    @Operation(summary = "List project students", description = "Returns all students assigned to the selected project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Students loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentProfile.class)))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/projects/{projectId}/students")
    public List<StudentProfile> getProjectStudents(@Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId) {
        return tutorDataService.getProjectStudents(projectId);
    }

    /**
     * Lists groups for a project.
     *
     * @param projectId route project id
     * @return group summaries
     */
    @Operation(summary = "List project groups", description = "Returns project groups with student counts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Groups loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectGroupSummary.class)))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/projects/{projectId}/groups")
    public List<ProjectGroupSummary> getProjectGroups(@Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId) {
        return tutorDataService.getProjectGroups(projectId);
    }

    /**
     * Creates a group in a project.
     *
     * @param projectId route project id
     * @param request create request
     * @return created group summary
     */
        @Operation(summary = "Create project group", description = "Creates a new group in the selected project.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Group created", content = @Content(schema = @Schema(implementation = ProjectGroupSummary.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/projects/{projectId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
        public ProjectGroupSummary createProjectGroup(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Group creation payload",
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\n  \"name\": \"Group A\"\n}"))
            ) @Valid @RequestBody ProjectGroupCreateRequest request
        ) {
        return tutorDataService.createProjectGroup(projectId, request);
    }

    /**
     * Deletes a project group and moves contained students to Ungrouped.
     *
     * @param projectId route project id
     * @param groupName route group name
     */
        @Operation(summary = "Delete project group", description = "Deletes one group and moves students to Ungrouped.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Group deleted"),
            @ApiResponse(responseCode = "400", description = "Illegal group operation", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @DeleteMapping("/projects/{projectId}/groups/{groupName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        public void deleteProjectGroup(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Group name", example = "Group A") @PathVariable String groupName
        ) {
        tutorDataService.deleteProjectGroup(projectId, groupName);
    }

    /**
     * Adds a student to the selected project.
     *
     * @param projectId route project id
     * @param request request body
     * @return created student profile
     */
        @Operation(summary = "Create project student", description = "Creates a student profile in a project.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student created", content = @Content(schema = @Schema(implementation = StudentProfile.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/projects/{projectId}/students")
    @ResponseStatus(HttpStatus.CREATED)
        public StudentProfile createProjectStudent(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Student creation payload",
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\n  \"name\": \"Alex Thompson\",\n  \"notes\": \"Needs additional practice on quadratics.\",\n  \"groupName\": \"Group A\"\n}"))
            ) @Valid @RequestBody StudentCreateRequest request
        ) {
        return tutorDataService.createProjectStudent(projectId, request);
    }

    /**
     * Updates notes for a student.
     *
     * @param studentId student key
     * @param request request body
     * @return updated student profile
     */
        @Operation(summary = "Update student notes", description = "Updates notes for one student.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student updated", content = @Content(schema = @Schema(implementation = StudentProfile.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PatchMapping("/students/{studentId}/notes")
        public StudentProfile updateStudentNotes(
            @Parameter(description = "Student key", example = "student-123") @PathVariable String studentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Notes update payload",
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\n  \"notes\": \"Improved in fractions and linear equations.\"\n}"))
            ) @Valid @RequestBody StudentNotesUpdateRequest request
        ) {
        return tutorDataService.updateStudentNotes(studentId, request);
    }

    /**
     * Updates group assignment for a student.
     *
     * @param studentId student key
     * @param request group update payload
     * @return updated student profile
     */
        @Operation(summary = "Update student group", description = "Reassigns a student to another group.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student updated", content = @Content(schema = @Schema(implementation = StudentProfile.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Student or project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PatchMapping("/students/{studentId}/group")
        public StudentProfile updateStudentGroup(
            @Parameter(description = "Student key", example = "student-123") @PathVariable String studentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Group update payload",
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\n  \"groupName\": \"Group B\"\n}"))
            ) @Valid @RequestBody StudentGroupUpdateRequest request
        ) {
        return tutorDataService.updateStudentGroup(studentId, request);
    }

    /**
     * Deletes one student.
     *
     * @param studentId student key
     */
    @Operation(summary = "Delete student", description = "Deletes a student profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Student deleted"),
            @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@Parameter(description = "Student key", example = "student-123") @PathVariable String studentId) {
        tutorDataService.deleteStudent(studentId);
    }

    /**
     * Returns report table rows.
     *
     * @return list of report rows
     */
    @Operation(summary = "List reports", description = "Returns report rows across all projects.")
    @ApiResponse(responseCode = "200", description = "Reports loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReportRow.class))))
    @GetMapping("/reports")
    public List<ReportRow> getReports() {
        return tutorDataService.getReports();
    }

    /**
     * Returns reports for a project.
     *
     * @param projectId route project id
     * @return project reports
     */
    @Operation(summary = "List project reports", description = "Returns report rows for one project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project reports loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReportRow.class)))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/projects/{projectId}/reports")
    public List<ReportRow> getProjectReports(@Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId) {
        return tutorDataService.getProjectReports(projectId);
    }

    /**
     * Generates or refreshes one monthly report for a project based on timeslots.
     *
     * @param projectId route project id
     * @param month month key in yyyy-MM format
     * @return generated report row
     */
        @Operation(summary = "Generate monthly report", description = "Generates or refreshes one monthly report for a project.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated", content = @Content(schema = @Schema(implementation = ReportRow.class))),
            @ApiResponse(responseCode = "400", description = "Invalid month or input", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/projects/{projectId}/reports/generate")
        public ReportRow generateProjectReport(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Month key in yyyy-MM format", example = "2026-03") @RequestParam String month
        ) {
        return tutorDataService.generateProjectMonthlyReport(projectId, month);
    }

    /**
     * Creates a timeslot used by the editor modal.
     *
     * @param request create request body
     * @return created timeslot
     */
        @Operation(summary = "Create global timeslot", description = "Legacy endpoint for creating a timeslot outside project route scope.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timeslot created", content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/timeslots")
        public TimeslotResponse createTimeslot(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Timeslot payload",
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\n  \"title\": \"Integration Workshop\",\n  \"description\": \"Focused revision session\",\n  \"durationMinutes\": 90,\n  \"date\": \"2026-03-20\",\n  \"startTime\": \"14:00\"\n}"))
            ) @Valid @RequestBody TimeslotCreateRequest request
        ) {
        return tutorDataService.createTimeslot(request);
    }

    /**
     * Lists timeslots for a project in an optional month window.
     *
     * @param projectId route project id
     * @param month optional month key in yyyy-MM format
     * @return matching timeslots
     */
        @Operation(summary = "List project timeslots", description = "Returns project timeslots, optionally filtered by month.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timeslots loaded", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimeslotResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/projects/{projectId}/timeslots")
    public List<TimeslotResponse> getProjectTimeslots(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Month key in yyyy-MM format", example = "2026-03") @RequestParam(required = false) String month
    ) {
        return tutorDataService.getProjectTimeslots(projectId, month);
    }

    /**
     * Creates a timeslot inside a project calendar.
     *
     * @param projectId route project id
     * @param request create request body
     * @return created timeslot
     */
        @Operation(summary = "Create project timeslot", description = "Creates a timeslot in project calendar context.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Timeslot created", content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/projects/{projectId}/timeslots")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeslotResponse createProjectTimeslot(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Valid @RequestBody TimeslotCreateRequest request
    ) {
        return tutorDataService.createProjectTimeslot(projectId, request);
    }

    /**
     * Returns one timeslot from a project calendar.
     *
     * @param projectId route project id
     * @param timeslotId route timeslot id
     * @return timeslot payload
     */
        @Operation(summary = "Get project timeslot", description = "Returns one timeslot by id in project context.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timeslot loaded", content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or timeslot not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/projects/{projectId}/timeslots/{timeslotId}")
        public TimeslotResponse getProjectTimeslot(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Timeslot identifier", example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14") @PathVariable String timeslotId
        ) {
        return tutorDataService.getProjectTimeslot(projectId, timeslotId);
    }

    /**
     * Updates one timeslot in a project calendar.
     *
     * @param projectId route project id
     * @param timeslotId route timeslot id
     * @param request update request
     * @return updated timeslot
     */
        @Operation(summary = "Update project timeslot", description = "Updates a single project timeslot.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timeslot updated", content = @Content(schema = @Schema(implementation = TimeslotResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or timeslot not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PutMapping("/projects/{projectId}/timeslots/{timeslotId}")
    public TimeslotResponse updateProjectTimeslot(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Timeslot identifier", example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14") @PathVariable String timeslotId,
            @Valid @RequestBody TimeslotCreateRequest request
    ) {
        return tutorDataService.updateProjectTimeslot(projectId, timeslotId, request);
    }

    /**
     * Deletes one timeslot in a project calendar.
     *
     * @param projectId route project id
     * @param timeslotId route timeslot id
     */
    @Operation(summary = "Delete project timeslot", description = "Deletes one timeslot from a project calendar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Timeslot deleted"),
            @ApiResponse(responseCode = "404", description = "Project or timeslot not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/projects/{projectId}/timeslots/{timeslotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectTimeslot(
            @Parameter(description = "Project slug", example = "math-grade-10") @PathVariable String projectId,
            @Parameter(description = "Timeslot identifier", example = "a6f265b7-f6af-4fd1-89b5-08f5b8872b14") @PathVariable String timeslotId
    ) {
        tutorDataService.deleteProjectTimeslot(projectId, timeslotId);
    }
}
