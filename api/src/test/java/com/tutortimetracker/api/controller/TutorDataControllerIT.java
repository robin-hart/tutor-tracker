package com.tutortimetracker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import com.tutortimetracker.api.repository.TodaySlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TutorDataControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectGroupRepository projectGroupRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TimeslotRepository timeslotRepository;
    @Autowired
    private TodaySlotRepository todaySlotRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
        timeslotRepository.deleteAll();
        todaySlotRepository.deleteAll();
        projectGroupRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void getProjects_shouldReturnPersistedProjects() throws Exception {
        ProjectEntity project = projectRepository.save(newProject("math-grade-10", "Math"));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").value(project.getSlug()));
    }

    @Test
    void createProject_shouldReturnCreatedProject() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Physics Advanced",
                "category", "SCIENCE",
                "totalHours", 0.0,
                "monthHours", 0.0,
                "completionPercent", 0
        );

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("physics-advanced"))
                .andExpect(jsonPath("$.name").value("Physics Advanced"));
    }

    @Test
    void createStudent_andGetStudents_shouldWorkWithInMemoryDatabase() throws Exception {
        ProjectEntity project = projectRepository.save(newProject("proj-a", "Project A"));
        ensureGroup(project, "Ungrouped");

        Map<String, Object> payload = Map.of(
                "name", "Alice",
                "notes", "  progressing well  "
        );

        mockMvc.perform(post("/api/projects/{projectId}/students", project.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.notes").value("progressing well"))
                .andExpect(jsonPath("$.groupName").value("Ungrouped"));

        mockMvc.perform(get("/api/projects/{projectId}/students", project.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void createStudent_shouldReturnNotFoundForUnknownProject() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Ghost",
                "notes", "n/a"
        );

        mockMvc.perform(post("/api/projects/{projectId}/students", "missing-project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found: missing-project"));
    }

    @Test
    void updateStudentNotes_shouldRejectBlankNotes() throws Exception {
        StudentEntity student = persistStudent("proj-a", "student-1", "Alice", "original", "Ungrouped");

        Map<String, Object> payload = Map.of("notes", "   ");

        mockMvc.perform(patch("/api/students/{studentId}/notes", student.getStudentKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStudentNotes_shouldPersistTrimmedValue() throws Exception {
        StudentEntity student = persistStudent("proj-a", "student-2", "Bob", "original", "Ungrouped");

        Map<String, Object> payload = Map.of("notes", "  updated note  ");

        mockMvc.perform(patch("/api/students/{studentId}/notes", student.getStudentKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("updated note"));
    }

    @Test
    void deleteStudent_shouldRemoveStudent() throws Exception {
        StudentEntity student = persistStudent("proj-a", "student-3", "Cara", "note", "Ungrouped");

        mockMvc.perform(delete("/api/students/{studentId}", student.getStudentKey()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/students", "proj-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

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

    private StudentEntity persistStudent(String projectSlug, String studentKey, String name, String notes, String groupName) {
        ProjectEntity project = projectRepository.findBySlug(projectSlug)
                .orElseGet(() -> projectRepository.save(newProject(projectSlug, "Project " + UUID.randomUUID())));
        ensureGroup(project, groupName);

        StudentEntity student = new StudentEntity();
        student.setStudentKey(studentKey);
        student.setName(name);
        student.setNotes(notes);
        student.setGroupName(groupName);
        student.setProject(project);
        return studentRepository.save(student);
    }
}
