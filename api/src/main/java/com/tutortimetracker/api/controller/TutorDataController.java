package com.tutortimetracker.api.controller;

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
 * Main REST API for TutorTimeTracker frontend consumption.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173"})
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
    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String projectId) {
        tutorDataService.deleteProject(projectId);
    }

    /**
     * Returns project calendar metadata and today's slots.
     *
     * @param projectId route project id
     * @return calendar payload
     */
    @GetMapping("/projects/{projectId}/calendar")
    public ProjectCalendarResponse getProjectCalendar(
            @PathVariable String projectId,
            @RequestParam(required = false) String month
    ) {
        return tutorDataService.getProjectCalendar(projectId, month);
    }

    /**
     * Returns student cards for a project.
     *
     * @param projectId route project id
     * @return students list
     */
    @GetMapping("/projects/{projectId}/students")
    public List<StudentProfile> getProjectStudents(@PathVariable String projectId) {
        return tutorDataService.getProjectStudents(projectId);
    }

    /**
     * Lists groups for a project.
     *
     * @param projectId route project id
     * @return group summaries
     */
    @GetMapping("/projects/{projectId}/groups")
    public List<ProjectGroupSummary> getProjectGroups(@PathVariable String projectId) {
        return tutorDataService.getProjectGroups(projectId);
    }

    /**
     * Creates a group in a project.
     *
     * @param projectId route project id
     * @param request create request
     * @return created group summary
     */
    @PostMapping("/projects/{projectId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectGroupSummary createProjectGroup(@PathVariable String projectId, @Valid @RequestBody ProjectGroupCreateRequest request) {
        return tutorDataService.createProjectGroup(projectId, request);
    }

    /**
     * Deletes a project group and moves contained students to Ungrouped.
     *
     * @param projectId route project id
     * @param groupName route group name
     */
    @DeleteMapping("/projects/{projectId}/groups/{groupName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectGroup(@PathVariable String projectId, @PathVariable String groupName) {
        tutorDataService.deleteProjectGroup(projectId, groupName);
    }

    /**
     * Adds a student to the selected project.
     *
     * @param projectId route project id
     * @param request request body
     * @return created student profile
     */
    @PostMapping("/projects/{projectId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfile createProjectStudent(@PathVariable String projectId, @Valid @RequestBody StudentCreateRequest request) {
        return tutorDataService.createProjectStudent(projectId, request);
    }

    /**
     * Updates notes for a student.
     *
     * @param studentId student key
     * @param request request body
     * @return updated student profile
     */
    @PatchMapping("/students/{studentId}/notes")
    public StudentProfile updateStudentNotes(@PathVariable String studentId, @Valid @RequestBody StudentNotesUpdateRequest request) {
        return tutorDataService.updateStudentNotes(studentId, request);
    }

    /**
     * Updates group assignment for a student.
     *
     * @param studentId student key
     * @param request group update payload
     * @return updated student profile
     */
    @PatchMapping("/students/{studentId}/group")
    public StudentProfile updateStudentGroup(@PathVariable String studentId, @Valid @RequestBody StudentGroupUpdateRequest request) {
        return tutorDataService.updateStudentGroup(studentId, request);
    }

    /**
     * Returns report table rows.
     *
     * @return list of report rows
     */
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
    @GetMapping("/projects/{projectId}/reports")
    public List<ReportRow> getProjectReports(@PathVariable String projectId) {
        return tutorDataService.getProjectReports(projectId);
    }

    /**
     * Generates or refreshes one monthly report for a project based on timeslots.
     *
     * @param projectId route project id
     * @param month month key in yyyy-MM format
     * @return generated report row
     */
    @PostMapping("/projects/{projectId}/reports/generate")
    public ReportRow generateProjectReport(@PathVariable String projectId, @RequestParam String month) {
        return tutorDataService.generateProjectMonthlyReport(projectId, month);
    }

    /**
     * Creates a timeslot used by the editor modal.
     *
     * @param request create request body
     * @return created timeslot
     */
    @PostMapping("/timeslots")
    public TimeslotResponse createTimeslot(@Valid @RequestBody TimeslotCreateRequest request) {
        return tutorDataService.createTimeslot(request);
    }

    /**
     * Lists timeslots for a project in an optional month window.
     *
     * @param projectId route project id
     * @param month optional month key in yyyy-MM format
     * @return matching timeslots
     */
    @GetMapping("/projects/{projectId}/timeslots")
    public List<TimeslotResponse> getProjectTimeslots(
            @PathVariable String projectId,
            @RequestParam(required = false) String month
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
    @PostMapping("/projects/{projectId}/timeslots")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeslotResponse createProjectTimeslot(
            @PathVariable String projectId,
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
    @GetMapping("/projects/{projectId}/timeslots/{timeslotId}")
    public TimeslotResponse getProjectTimeslot(@PathVariable String projectId, @PathVariable String timeslotId) {
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
    @PutMapping("/projects/{projectId}/timeslots/{timeslotId}")
    public TimeslotResponse updateProjectTimeslot(
            @PathVariable String projectId,
            @PathVariable String timeslotId,
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
    @DeleteMapping("/projects/{projectId}/timeslots/{timeslotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectTimeslot(@PathVariable String projectId, @PathVariable String timeslotId) {
        tutorDataService.deleteProjectTimeslot(projectId, timeslotId);
    }
}
