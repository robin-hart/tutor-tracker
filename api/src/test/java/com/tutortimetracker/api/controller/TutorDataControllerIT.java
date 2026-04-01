package com.tutortimetracker.api.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.ReportRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import com.tutortimetracker.api.repository.TodaySlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TutorDataControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectGroupRepository projectGroupRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private TimeslotRepository timeslotRepository;
  @Autowired private TodaySlotRepository todaySlotRepository;
  @Autowired private ReportRepository reportRepository;

  @BeforeEach
  void cleanDatabase() {
    studentRepository.deleteAll();
    timeslotRepository.deleteAll();
    todaySlotRepository.deleteAll();
    reportRepository.deleteAll();
    projectGroupRepository.deleteAll();
    projectRepository.deleteAll();
  }

  @Test
  void getProjects_shouldReturnPersistedProjects() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("math-grade-10", "Math"));

    mockMvc
        .perform(get("/api/projects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$[0].id").value(project.getSlug()));
  }

  @Test
  void createProject_shouldReturnCreatedProject() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "name",
            "Physics Advanced",
            "category",
            "SCIENCE",
            "totalHours",
            0.0,
            "monthHours",
            0.0,
            "completionPercent",
            0);

    mockMvc
        .perform(
            post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("physics-advanced"))
        .andExpect(jsonPath("$.name").value("Physics Advanced"));
  }

  @Test
  void deleteProject_shouldRemoveProjectAndAssociatedData() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-to-delete", "Project"));
    ensureGroup(project, "Ungrouped");
    persistStudent(project.getSlug(), "s1", "Alice", "note", "Ungrouped");

    mockMvc
        .perform(delete("/api/projects/{projectId}", project.getSlug()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/projects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == 'proj-to-delete')]").doesNotExist());
  }

  @Test
  void deleteProject_shouldReturnNotFoundForMissingProject() throws Exception {
    mockMvc.perform(delete("/api/projects/missing-project")).andExpect(status().isNotFound());
  }

  @Test
  void createStudent_andGetStudents_shouldWorkWithInMemoryDatabase() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-a", "Project A"));
    ensureGroup(project, "Ungrouped");

    Map<String, Object> payload =
        Map.of(
            "name", "Alice",
            "notes", "  progressing well  ");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/students", project.getSlug())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.notes").value("progressing well"))
        .andExpect(jsonPath("$.groupName").value("Ungrouped"));

    mockMvc
        .perform(get("/api/projects/{projectId}/students", project.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Alice"));
  }

  @Test
  void createStudent_shouldReturnNotFoundForUnknownProject() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "name", "Ghost",
            "notes", "n/a");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/students", "missing-project")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: missing-project"));
  }

  @Test
  void updateStudentNotes_shouldRejectBlankNotes() throws Exception {
    StudentEntity student = persistStudent("proj-a", "student-1", "Alice", "original", "Ungrouped");

    Map<String, Object> payload = Map.of("notes", "   ");

    mockMvc
        .perform(
            patch("/api/students/{studentId}/notes", student.getStudentKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateStudentNotes_shouldPersistTrimmedValue() throws Exception {
    StudentEntity student = persistStudent("proj-a", "student-2", "Bob", "original", "Ungrouped");

    Map<String, Object> payload = Map.of("notes", "  updated note  ");

    mockMvc
        .perform(
            patch("/api/students/{studentId}/notes", student.getStudentKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("updated note"));
  }

  @Test
  void updateStudentNotes_shouldReturnNotFoundForMissingStudent() throws Exception {
    Map<String, Object> payload = Map.of("notes", "some notes");

    mockMvc
        .perform(
            patch("/api/students/{studentId}/notes", "missing-student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteStudent_shouldRemoveStudent() throws Exception {
    StudentEntity student = persistStudent("proj-a", "student-3", "Cara", "note", "Ungrouped");

    mockMvc
        .perform(delete("/api/students/{studentId}", student.getStudentKey()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/projects/{projectId}/students", "proj-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void deleteStudent_shouldReturnNotFoundForMissingStudent() throws Exception {
    mockMvc.perform(delete("/api/students/missing-student")).andExpect(status().isNotFound());
  }

  @Test
  void getProjectGroups_shouldReturnGroupsWithCounts() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-groups", "Groups Project"));
    ensureGroup(project, "Ungrouped");
    ensureGroup(project, "Advanced");
    persistStudent(project.getSlug(), "s1", "Alice", "note", "Ungrouped");
    persistStudent(project.getSlug(), "s2", "Bob", "note", "Advanced");
    persistStudent(project.getSlug(), "s3", "Charlie", "note", "Advanced");

    mockMvc
        .perform(get("/api/projects/{projectId}/groups", project.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.name == 'Advanced')].studentCount").isArray());
  }

  @Test
  void createProjectGroup_shouldCreateNewGroup() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-new-group", "Project"));

    Map<String, String> payload = Map.of("name", "Honors");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/groups", project.getSlug())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Honors"))
        .andExpect(jsonPath("$.studentCount").value(0));
  }

  @Test
  void createProjectGroup_shouldReturnNotFoundForMissingProject() throws Exception {
    Map<String, String> payload = Map.of("name", "Honors");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/groups", "missing-project")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteProjectGroup_shouldReassignStudents() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-delete-group", "Project"));
    ensureGroup(project, "Ungrouped");
    ensureGroup(project, "ToDelete");
    persistStudent(project.getSlug(), "s1", "Alice", "note", "ToDelete");

    mockMvc
        .perform(
            delete("/api/projects/{projectId}/groups/{groupName}", project.getSlug(), "ToDelete"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/projects/{projectId}/students", project.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].groupName").value("Ungrouped"));
  }

  @Test
  void deleteProjectGroup_shouldRejectDeletingUngrouped() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-ungrouped", "Project"));
    ensureGroup(project, "Ungrouped");

    mockMvc
        .perform(
            delete("/api/projects/{projectId}/groups/{groupName}", project.getSlug(), "Ungrouped"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Ungrouped cannot be deleted."));
  }

  @Test
  void updateStudentGroup_shouldChangeGroupAssignment() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-update-group", "Project"));
    ensureGroup(project, "Ungrouped");
    ensureGroup(project, "Advanced");
    StudentEntity student = persistStudent(project.getSlug(), "s1", "Alice", "note", "Ungrouped");

    Map<String, String> payload = Map.of("groupName", "Advanced");

    mockMvc
        .perform(
            patch("/api/students/{studentId}/group", student.getStudentKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupName").value("Advanced"));
  }

  @Test
  void updateStudentGroup_shouldReturnNotFoundForMissingStudent() throws Exception {
    Map<String, String> payload = Map.of("groupName", "Advanced");

    mockMvc
        .perform(
            patch("/api/students/{studentId}/group", "missing-student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProjectCalendar_shouldReturnCalendarData() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-calendar", "Calendar Project"));

    LocalDate today = LocalDate.now();
    createTimeslot(project, "Today Session", today, 60);
    createTimeslot(project, "Future Session", today.plusDays(5), 90);

    mockMvc
        .perform(get("/api/projects/{projectId}/calendar", project.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectName").value("Calendar Project"))
        .andExpect(jsonPath("$.totalHours").value(2.5))
        .andExpect(jsonPath("$.monthHours").value(2.5));
  }

  @Test
  void getProjectCalendar_shouldReturnNotFoundForMissingProject() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectId}/calendar", "missing-project"))
        .andExpect(status().isNotFound());
  }

  @Test
  void createProjectTimeslot_shouldCreateTimeslot() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-timeslot", "Project"));

    Map<String, Object> payload =
        Map.of(
            "title", "Session 1",
            "description", "notes",
            "durationMinutes", 60,
            "date", LocalDate.now().toString(),
            "startTime", "14:00:00");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/timeslots", project.getSlug())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Session 1"))
        .andExpect(jsonPath("$.durationMinutes").value(60));
  }

  @Test
  void createProjectTimeslot_shouldReturnNotFoundForMissingProject() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "title",
            "Session",
            "durationMinutes",
            60,
            "date",
            LocalDate.now().toString(),
            "startTime",
            "14:00:00");

    mockMvc
        .perform(
            post("/api/projects/{projectId}/timeslots", "missing-project")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProjectTimeslots_shouldReturnTimeslotsInMonth() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-month-slots", "Project"));
    createTimeslot(project, "Session 1", LocalDate.of(2026, 1, 10), 60);
    createTimeslot(project, "Session 2", LocalDate.of(2026, 1, 20), 90);

    mockMvc
        .perform(
            get("/api/projects/{projectId}/timeslots", project.getSlug()).param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getProjectTimeslot_shouldReturnSingleTimeslot() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-single-slot", "Project"));
    TimeslotEntity slot = createTimeslot(project, "Session", LocalDate.now(), 60);

    mockMvc
        .perform(
            get(
                "/api/projects/{projectId}/timeslots/{timeslotId}",
                project.getSlug(),
                slot.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Session"))
        .andExpect(jsonPath("$.durationMinutes").value(60));
  }

  @Test
  void getProjectTimeslot_shouldReturnNotFoundForMissingSlot() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-missing-slot", "Project"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/timeslots/{timeslotId}", project.getSlug(), "missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateProjectTimeslot_shouldUpdateTimeslot() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-update-slot", "Project"));
    TimeslotEntity slot = createTimeslot(project, "Old Title", LocalDate.now(), 60);

    Map<String, Object> payload =
        Map.of(
            "title", "New Title",
            "description", "updated",
            "durationMinutes", 90,
            "date", LocalDate.now().toString(),
            "startTime", "15:00:00");

    mockMvc
        .perform(
            put("/api/projects/{projectId}/timeslots/{timeslotId}", project.getSlug(), slot.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.durationMinutes").value(90));
  }

  @Test
  void deleteProjectTimeslot_shouldRemoveTimeslot() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-delete-slot", "Project"));
    TimeslotEntity slot = createTimeslot(project, "To Delete", LocalDate.now(), 60);

    mockMvc
        .perform(
            delete(
                "/api/projects/{projectId}/timeslots/{timeslotId}",
                project.getSlug(),
                slot.getId()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(
                "/api/projects/{projectId}/timeslots/{timeslotId}",
                project.getSlug(),
                slot.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getReports_shouldReturnAllReports() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-report", "Project"));
    createTimeslot(project, "Session", LocalDate.of(2026, 1, 10), 60);

    mockMvc
        .perform(
            post("/api/projects/{projectId}/reports/generate", project.getSlug())
                .param("month", "2026-01"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/reports"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
  }

  @Test
  void getProjectReports_shouldReturnProjectOnlyReports() throws Exception {
    ProjectEntity project1 = projectRepository.save(newProject("proj-report-1", "Project 1"));
    ProjectEntity project2 = projectRepository.save(newProject("proj-report-2", "Project 2"));

    createTimeslot(project1, "Session", LocalDate.of(2026, 1, 10), 60);
    createTimeslot(project2, "Session", LocalDate.of(2026, 1, 10), 90);

    mockMvc.perform(
        post("/api/projects/{projectId}/reports/generate", project1.getSlug())
            .param("month", "2026-01"));
    mockMvc.perform(
        post("/api/projects/{projectId}/reports/generate", project2.getSlug())
            .param("month", "2026-01"));

    mockMvc
        .perform(get("/api/projects/{projectId}/reports", project1.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].projectName").value("Project 1"));
  }

  @Test
  void generateProjectReport_shouldCreateReportFromTimeslots() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-gen-report", "Project"));
    project.setName("Math Tutoring");
    projectRepository.save(project);

    createTimeslot(project, "Session 1", LocalDate.of(2026, 1, 10), 60);
    createTimeslot(project, "Session 2", LocalDate.of(2026, 1, 20), 90);

    mockMvc
        .perform(
            post("/api/projects/{projectId}/reports/generate", project.getSlug())
                .param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectName").value("Math Tutoring"))
        .andExpect(jsonPath("$.totalHours").value(2.5))
        .andExpect(jsonPath("$.sessions").value(2))
        .andExpect(jsonPath("$.grossAmount").value(150.0));
  }

  @Test
  void generateProjectReport_shouldReturnNotFoundForMissingProject() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/{projectId}/reports/generate", "missing-project")
                .param("month", "2026-01"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProjectReports_shouldReturnNotFoundForMissingProject() throws Exception {
    mockMvc
        .perform(get("/api/projects/{projectId}/reports", "missing-project"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: missing-project"));
  }

  @Test
  void exportProjectReportPdf_shouldReturnBadRequestForInvalidMonth() throws Exception {
    ProjectEntity project = projectRepository.save(newProject("proj-export-invalid", "Project"));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/reports/export/pdf", project.getSlug())
                .param("month", "2026-99"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.path").value("/api/projects/proj-export-invalid/reports/export/pdf"));
  }

  @Test
  void exportProjectReportPdf_shouldReturnNotFoundForMissingProject() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/{projectId}/reports/export/pdf", "missing-project")
                .param("month", "2026-01"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: missing-project"));
  }

  @Test
  void createTimeslot_shouldCreateGlobalTimeslot() throws Exception {
    // Ensure the default project exists
    projectRepository
        .findBySlug("math-grade-10")
        .orElseGet(() -> projectRepository.save(newProject("math-grade-10", "Math Grade 10")));

    Map<String, Object> payload =
        Map.of(
            "title", "Global Session",
            "description", "notes",
            "durationMinutes", 60,
            "date", LocalDate.now().toString(),
            "startTime", "10:00:00");

    mockMvc
        .perform(
            post("/api/timeslots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Global Session"));
  }

  // Helper methods

  private ProjectEntity newProject(String slug, String name) {
    ProjectEntity project = new ProjectEntity();
    project.setSlug(slug);
    project.setName(name);
    project.setCategory("GENERAL");
    project.setTotalHours(0.0);
    project.setMonthHours(0.0);
    project.setCompletionPercent(0);
    return project;
  }

  private void ensureGroup(ProjectEntity project, String groupName) {
    ProjectGroupEntity group = new ProjectGroupEntity();
    group.setName(groupName);
    group.setProject(project);
    projectGroupRepository.save(group);
  }

  private StudentEntity persistStudent(
      String projectSlug, String studentKey, String name, String notes, String groupName) {
    ProjectEntity project =
        projectRepository
            .findBySlug(projectSlug)
            .orElseGet(
                () ->
                    projectRepository.save(
                        newProject(projectSlug, "Project " + UUID.randomUUID())));

    // Check if group already exists before creating
    if (projectGroupRepository.findByProjectAndName(project, groupName).isEmpty()) {
      ensureGroup(project, groupName);
    }

    StudentEntity student = new StudentEntity();
    student.setStudentKey(studentKey);
    student.setName(name);
    student.setNotes(notes);
    student.setGroupName(groupName);
    student.setProject(project);
    return studentRepository.save(student);
  }

  private TimeslotEntity createTimeslot(
      ProjectEntity project, String title, LocalDate date, int durationMinutes) {
    TimeslotEntity slot = new TimeslotEntity();
    slot.setId(UUID.randomUUID().toString());
    slot.setTitle(title);
    slot.setDate(date);
    slot.setStartTime(LocalTime.of(14, 0));
    slot.setDurationMinutes(durationMinutes);
    slot.setProject(project);
    return timeslotRepository.save(slot);
  }
}
