package com.tutortimetracker.api.service;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.ReportRowEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.entity.TodaySlotEntity;
import com.tutortimetracker.api.model.ProjectCreateRequest;
import com.tutortimetracker.api.model.ProjectSummary;
import com.tutortimetracker.api.model.StudentCreateRequest;
import com.tutortimetracker.api.model.StudentNotesUpdateRequest;
import com.tutortimetracker.api.model.StudentProfile;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.ReportRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import com.tutortimetracker.api.repository.TodaySlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorDataServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectGroupRepository projectGroupRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private TimeslotRepository timeslotRepository;
    @Mock
    private TodaySlotRepository todaySlotRepository;

    private TutorDataService service;

    @BeforeEach
    void setUp() {
        service = new TutorDataService(
                projectRepository,
                projectGroupRepository,
                studentRepository,
                reportRepository,
                timeslotRepository,
                todaySlotRepository
        );
    }

    @Test
    void createProject_shouldGenerateUniqueSlugAndCreateDefaultGroup() {
        ProjectEntity duplicate = new ProjectEntity();
        duplicate.setSlug("math-grade-10");

        when(projectRepository.findBySlug("math-grade-10")).thenReturn(Optional.of(duplicate));
        when(projectRepository.findBySlug("math-grade-10-2")).thenReturn(Optional.empty());
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectGroupRepository.findByProjectAndName(any(ProjectEntity.class), eq("Ungrouped"))).thenReturn(Optional.empty());

        ProjectSummary summary = service.createProject(
                new ProjectCreateRequest("Math Grade 10", "STEM", 0.0, 0.0, 0)
        );

        assertEquals("math-grade-10-2", summary.id());
        assertEquals("Math Grade 10", summary.name());
        verify(projectGroupRepository).save(any(ProjectGroupEntity.class));
    }

    @Test
    void createProjectStudent_shouldTrimNotesAndUseUngroupedByDefault() {
        ProjectEntity project = new ProjectEntity();
        project.setSlug("project-1");

        when(projectRepository.findBySlug("project-1")).thenReturn(Optional.of(project));
        when(projectGroupRepository.findByProjectAndName(project, "Ungrouped")).thenReturn(Optional.of(new ProjectGroupEntity()));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfile profile = service.createProjectStudent(
                "project-1",
                new StudentCreateRequest("Alice", "  started chapter 3  ", null)
        );

        assertEquals("Alice", profile.name());
        assertEquals("started chapter 3", profile.notes());
        assertEquals("Ungrouped", profile.groupName());

        ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
        verify(studentRepository).save(captor.capture());
        assertEquals("Ungrouped", captor.getValue().getGroupName());
    }

    @Test
    void updateStudentNotes_shouldTrimAndPersist() {
        StudentEntity entity = new StudentEntity();
        entity.setStudentKey("student-1");
        entity.setName("Alice");
        entity.setNotes("old");
        entity.setGroupName("Ungrouped");

        when(studentRepository.findByStudentKey("student-1")).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfile updated = service.updateStudentNotes("student-1", new StudentNotesUpdateRequest("  new notes  "));

        assertEquals("new notes", updated.notes());
        assertEquals("new notes", entity.getNotes());
    }

    @Test
    void deleteStudent_shouldThrowWhenMissing() {
        when(studentRepository.findByStudentKey("missing")).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> service.deleteStudent("missing"));
        verify(studentRepository, never()).delete(any(StudentEntity.class));
    }

    @Test
    void deleteProject_shouldDeleteAssociatedDataThenProject() {
        ProjectEntity project = new ProjectEntity();
        project.setSlug("project-1");

        List<StudentEntity> students = List.of(new StudentEntity());
        List<TimeslotEntity> timeslots = List.of(new TimeslotEntity());
        List<TodaySlotEntity> todaySlots = List.of(new TodaySlotEntity());
        List<ProjectGroupEntity> groups = List.of(new ProjectGroupEntity());
        List<ReportRowEntity> reports = List.of(new ReportRowEntity());

        when(projectRepository.findBySlug("project-1")).thenReturn(Optional.of(project));
        when(studentRepository.findByProject(project)).thenReturn(students);
        when(timeslotRepository.findByProject(project)).thenReturn(timeslots);
        when(todaySlotRepository.findByProject(project)).thenReturn(todaySlots);
        when(projectGroupRepository.findByProject(project)).thenReturn(groups);
        when(reportRepository.findByProject(project)).thenReturn(reports);

        service.deleteProject("project-1");

        verify(studentRepository).deleteAll(students);
        verify(timeslotRepository).deleteAll(timeslots);
        verify(todaySlotRepository).deleteAll(todaySlots);
        verify(projectGroupRepository).deleteAll(groups);
        verify(reportRepository).deleteAll(reports);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectGroup_shouldRejectUngrouped() {
        ProjectEntity project = new ProjectEntity();
        project.setSlug("project-1");
        when(projectRepository.findBySlug("project-1")).thenReturn(Optional.of(project));

        assertThrows(IllegalArgumentException.class, () -> service.deleteProjectGroup("project-1", "Ungrouped"));
        verify(projectGroupRepository, never()).delete(any(ProjectGroupEntity.class));
    }
}
