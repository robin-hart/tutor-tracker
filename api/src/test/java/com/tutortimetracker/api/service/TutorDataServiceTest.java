package com.tutortimetracker.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.ReportRowEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.entity.TodaySlotEntity;
import com.tutortimetracker.api.model.ProjectCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupSummary;
import com.tutortimetracker.api.model.ProjectSummary;
import com.tutortimetracker.api.model.StudentCreateRequest;
import com.tutortimetracker.api.model.StudentGroupUpdateRequest;
import com.tutortimetracker.api.model.StudentNotesUpdateRequest;
import com.tutortimetracker.api.model.StudentProfile;
import com.tutortimetracker.api.model.TimeslotCreateRequest;
import com.tutortimetracker.api.model.TimeslotResponse;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.ReportRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import com.tutortimetracker.api.repository.TodaySlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TutorDataServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectGroupRepository projectGroupRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private ReportRepository reportRepository;
  @Mock private TimeslotRepository timeslotRepository;
  @Mock private TodaySlotRepository todaySlotRepository;

  private TutorDataService service;

  @BeforeEach
  void setUp() {
    service =
        new TutorDataService(
            projectRepository,
            projectGroupRepository,
            studentRepository,
            reportRepository,
            timeslotRepository,
            todaySlotRepository);
  }

  @Test
  void createProject_shouldGenerateUniqueSlugAndCreateDefaultGroup() {
    ProjectEntity duplicate = new ProjectEntity();
    duplicate.setSlug("math-grade-10");

    when(projectRepository.findBySlug("math-grade-10")).thenReturn(Optional.of(duplicate));
    when(projectRepository.findBySlug("math-grade-10-2")).thenReturn(Optional.empty());
    when(projectRepository.save(any(ProjectEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(projectGroupRepository.findByProjectAndName(any(ProjectEntity.class), eq("Ungrouped")))
        .thenReturn(Optional.empty());

    ProjectSummary summary =
        service.createProject(new ProjectCreateRequest("Math Grade 10", "STEM", 0.0, 0.0, 0));

    assertEquals("math-grade-10-2", summary.id());
    assertEquals("Math Grade 10", summary.name());
    verify(projectGroupRepository).save(any(ProjectGroupEntity.class));
  }

  @Test
  void createProject_shouldRetryWhenInsertCollidesOnSlug() {
    when(projectRepository.findBySlug("timeslot-test-project")).thenReturn(Optional.empty());
    when(projectRepository.save(any(ProjectEntity.class)))
        .thenThrow(
            new DataIntegrityViolationException(
                "Duplicate entry 'timeslot-test-project' for key 'project_slug'"))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(projectGroupRepository.findByProjectAndName(any(ProjectEntity.class), eq("Ungrouped")))
        .thenReturn(Optional.empty());

    ProjectSummary summary =
        service.createProject(
            new ProjectCreateRequest("Timeslot Test Project", "GENERAL", 0.0, 0.0, 0));

    assertTrue(summary.id().startsWith("timeslot-test-project"));
    verify(projectRepository, times(2)).save(any(ProjectEntity.class));
    verify(projectGroupRepository).save(any(ProjectGroupEntity.class));
  }

  @Test
  void createProjectStudent_shouldTrimNotesAndUseUngroupedByDefault() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("project-1");

    when(projectRepository.findBySlug("project-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Ungrouped"))
        .thenReturn(Optional.of(new ProjectGroupEntity()));
    when(studentRepository.save(any(StudentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StudentProfile profile =
        service.createProjectStudent(
            "project-1", new StudentCreateRequest("Alice", "  started chapter 3  ", null));

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
    when(studentRepository.save(any(StudentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StudentProfile updated =
        service.updateStudentNotes("student-1", new StudentNotesUpdateRequest("  new notes  "));

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

    assertThrows(
        IllegalArgumentException.class, () -> service.deleteProjectGroup("project-1", "Ungrouped"));
    verify(projectGroupRepository, never()).delete(any(ProjectGroupEntity.class));
  }

  @Test
  void getProjects_shouldReturnEmptyListWhenNoProjects() {
    when(projectRepository.findAll()).thenReturn(List.of());

    List<ProjectSummary> projects = service.getProjects();

    assertEquals(0, projects.size());
  }

  @Test
  void getProjects_shouldReturnAllProjects() {
    ProjectEntity proj1 = new ProjectEntity();
    proj1.setSlug("proj-1");
    proj1.setName("Project 1");
    proj1.setCategory("STEM");
    proj1.setTotalHours(100.0);
    proj1.setMonthHours(20.0);
    proj1.setCompletionPercent(50);

    ProjectEntity proj2 = new ProjectEntity();
    proj2.setSlug("proj-2");
    proj2.setName("Project 2");
    proj2.setCategory("HUMANITIES");
    proj2.setTotalHours(150.0);
    proj2.setMonthHours(30.0);
    proj2.setCompletionPercent(75);

    when(projectRepository.findAll()).thenReturn(List.of(proj1, proj2));

    List<ProjectSummary> projects = service.getProjects();

    assertEquals(2, projects.size());
    assertEquals("proj-1", projects.get(0).id());
    assertEquals("Project 1", projects.get(0).name());
    assertEquals("proj-2", projects.get(1).id());
  }

  @Test
  void getProjectStudents_shouldReturnAllStudentsInGroup() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    StudentEntity student1 = new StudentEntity();
    student1.setStudentKey("s1");
    student1.setName("Alice");
    student1.setNotes("progressing");
    student1.setGroupName("Ungrouped");
    student1.setProject(project);

    StudentEntity student2 = new StudentEntity();
    student2.setStudentKey("s2");
    student2.setName("Bob");
    student2.setNotes("stuck");
    student2.setGroupName("Group A");
    student2.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Ungrouped"))
        .thenReturn(Optional.of(new ProjectGroupEntity()));
    when(studentRepository.findByProject(project)).thenReturn(List.of(student1, student2));

    List<StudentProfile> students = service.getProjectStudents("proj-1");

    assertEquals(2, students.size());
    assertEquals("Alice", students.get(0).name());
    assertEquals("Bob", students.get(1).name());
  }

  @Test
  void getProjectGroups_shouldReturnGroupsWithCounts() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    StudentEntity s1 = new StudentEntity();
    s1.setGroupName("Ungrouped");
    s1.setProject(project);

    StudentEntity s2 = new StudentEntity();
    s2.setGroupName("Group A");
    s2.setProject(project);

    StudentEntity s3 = new StudentEntity();
    s3.setGroupName("Group A");
    s3.setProject(project);

    ProjectGroupEntity groupA = new ProjectGroupEntity();
    groupA.setName("Group A");
    groupA.setProject(project);

    ProjectGroupEntity ungrouped = new ProjectGroupEntity();
    ungrouped.setName("Ungrouped");
    ungrouped.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Ungrouped"))
        .thenReturn(Optional.of(ungrouped));
    when(studentRepository.findByProject(project)).thenReturn(List.of(s1, s2, s3));
    when(projectGroupRepository.findByProject(project)).thenReturn(List.of(groupA, ungrouped));

    List<ProjectGroupSummary> groups = service.getProjectGroups("proj-1");

    assertEquals(2, groups.size());
    assertTrue(groups.stream().anyMatch(g -> "Group A".equals(g.name()) && g.studentCount() == 2));
    assertTrue(
        groups.stream().anyMatch(g -> "Ungrouped".equals(g.name()) && g.studentCount() == 1));
  }

  @Test
  void createProjectGroup_shouldCreateNewGroup() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Honors"))
        .thenReturn(Optional.empty());
    when(projectGroupRepository.save(any(ProjectGroupEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(studentRepository.findByProjectAndGroupName(project, "Honors")).thenReturn(List.of());

    ProjectGroupSummary result =
        service.createProjectGroup("proj-1", new ProjectGroupCreateRequest("Honors"));

    assertEquals("Honors", result.name());
    assertEquals(0, result.studentCount());
    verify(projectGroupRepository).save(any(ProjectGroupEntity.class));
  }

  @Test
  void createProjectGroup_shouldReturnExistingGroupWithCounts() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    StudentEntity s1 = new StudentEntity();
    s1.setGroupName("Honors");

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Honors"))
        .thenReturn(Optional.of(new ProjectGroupEntity()));
    when(studentRepository.findByProjectAndGroupName(project, "Honors")).thenReturn(List.of(s1));

    ProjectGroupSummary result =
        service.createProjectGroup("proj-1", new ProjectGroupCreateRequest("Honors"));

    assertEquals("Honors", result.name());
    assertEquals(1, result.studentCount());
    verify(projectGroupRepository, never()).save(any(ProjectGroupEntity.class));
  }

  @Test
  void deleteProjectGroup_shouldReassignStudentsToUngrouped() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    StudentEntity s1 = new StudentEntity();
    s1.setStudentKey("s1");
    s1.setGroupName("Advanced");

    StudentEntity s2 = new StudentEntity();
    s2.setStudentKey("s2");
    s2.setGroupName("Advanced");

    ProjectGroupEntity advancedGroup = new ProjectGroupEntity();
    advancedGroup.setName("Advanced");
    advancedGroup.setProject(project);

    ProjectGroupEntity ungrouped = new ProjectGroupEntity();
    ungrouped.setName("Ungrouped");
    ungrouped.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(projectGroupRepository.findByProjectAndName(project, "Advanced"))
        .thenReturn(Optional.of(advancedGroup));
    when(studentRepository.findByProjectAndGroupName(project, "Advanced"))
        .thenReturn(List.of(s1, s2));
    when(projectGroupRepository.findByProjectAndName(project, "Ungrouped"))
        .thenReturn(Optional.of(ungrouped));

    service.deleteProjectGroup("proj-1", "Advanced");

    verify(studentRepository).saveAll(List.of(s1, s2));
    assertEquals("Ungrouped", s1.getGroupName());
    assertEquals("Ungrouped", s2.getGroupName());
    verify(projectGroupRepository).delete(advancedGroup);
  }

  @Test
  void updateStudentGroup_shouldChangeGroupAssignment() {
    StudentEntity student = new StudentEntity();
    student.setStudentKey("s1");
    student.setName("Alice");
    student.setGroupName("Ungrouped");

    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");
    student.setProject(project);

    ProjectGroupEntity groupB = new ProjectGroupEntity();
    groupB.setName("Group B");
    groupB.setProject(project);

    when(studentRepository.findByStudentKey("s1")).thenReturn(Optional.of(student));
    when(projectGroupRepository.findByProjectAndName(project, "Group B"))
        .thenReturn(Optional.of(groupB));
    when(studentRepository.save(any(StudentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StudentProfile result =
        service.updateStudentGroup("s1", new StudentGroupUpdateRequest("Group B"));

    assertEquals("Group B", result.groupName());
    assertEquals("Group B", student.getGroupName());
    verify(studentRepository).save(student);
  }

  @Test
  void updateStudentGroup_shouldThrowWhenStudentNotFound() {
    when(studentRepository.findByStudentKey("missing")).thenReturn(Optional.empty());
    StudentGroupUpdateRequest request = new StudentGroupUpdateRequest("Group A");

    assertThrows(
        StudentNotFoundException.class, () -> service.updateStudentGroup("missing", request));
    verify(studentRepository, never()).save(any(StudentEntity.class));
  }

  @Test
  void updateStudentNotes_shouldThrowWhenStudentNotFound() {
    when(studentRepository.findByStudentKey("missing")).thenReturn(Optional.empty());
    StudentNotesUpdateRequest request = new StudentNotesUpdateRequest("note");

    assertThrows(
        StudentNotFoundException.class, () -> service.updateStudentNotes("missing", request));
  }

  @Test
  void getReports_shouldReturnAllReports() {
    ReportRowEntity report1 = new ReportRowEntity();
    report1.setMonthKey("2026-01");
    report1.setMonth("January 2026");
    report1.setProjectName("Math");
    report1.setTotalHours(10.0);
    report1.setSessions(5);
    report1.setGrossAmount(600.0);

    ReportRowEntity report2 = new ReportRowEntity();
    report2.setMonthKey("2026-02");
    report2.setMonth("February 2026");
    report2.setProjectName("Physics");
    report2.setTotalHours(15.0);
    report2.setSessions(8);
    report2.setGrossAmount(900.0);

    when(reportRepository.findAll()).thenReturn(List.of(report1, report2));

    List<com.tutortimetracker.api.model.ReportRow> reports = service.getReports();

    assertEquals(2, reports.size());
  }

  @Test
  void getProjectReports_shouldReturnProjectSpecificReports() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    ReportRowEntity report = new ReportRowEntity();
    report.setProject(project);
    report.setMonthKey("2026-01");

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(reportRepository.findByProject(project)).thenReturn(List.of(report));

    List<com.tutortimetracker.api.model.ReportRow> reports = service.getProjectReports("proj-1");

    assertEquals(1, reports.size());
  }

  @Test
  void generateProjectMonthlyReport_shouldCalculateHoursAndAmountFromTimeslots() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");
    project.setName("Math Tutoring");

    TimeslotEntity slot1 = new TimeslotEntity();
    slot1.setDurationMinutes(60);

    TimeslotEntity slot2 = new TimeslotEntity();
    slot2.setDurationMinutes(90);

    YearMonth month = YearMonth.parse("2026-01");
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to))
        .thenReturn(List.of(slot1, slot2));
    when(reportRepository.findByProjectAndMonthKey(project, "2026-01"))
        .thenReturn(Optional.empty());
    when(reportRepository.save(any(ReportRowEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    com.tutortimetracker.api.model.ReportRow result =
        service.generateProjectMonthlyReport("proj-1", "2026-01");

    assertEquals(2.5, result.totalHours(), 0.001); // 150 minutes = 2.5 hours
    assertEquals(2, result.sessions());
    assertEquals(150.0, result.grossAmount(), 0.001); // 2.5 * 60 = 150
  }

  @Test
  void createProjectTimeslot_shouldSaveTimeslotAndRefreshMetrics() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.save(any(TimeslotEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TimeslotCreateRequest request =
        new TimeslotCreateRequest(
            "Session 1", "notes", 60, LocalDate.of(2026, 1, 15), LocalTime.of(14, 0));

    TimeslotResponse result = service.createProjectTimeslot("proj-1", request);

    assertEquals("Session 1", result.title());
    assertEquals(60, result.durationMinutes());
    verify(timeslotRepository).save(any(TimeslotEntity.class));
  }

  @Test
  void getProjectTimeslot_shouldReturnTimeslotDetails() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    TimeslotEntity slot = new TimeslotEntity();
    slot.setId("slot-1");
    slot.setTitle("Session 1");
    slot.setDurationMinutes(60);
    slot.setDate(LocalDate.of(2026, 1, 15));
    slot.setStartTime(LocalTime.of(14, 0));
    slot.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndId(project, "slot-1")).thenReturn(Optional.of(slot));

    TimeslotResponse result = service.getProjectTimeslot("proj-1", "slot-1");

    assertEquals("Session 1", result.title());
    assertEquals(LocalDate.of(2026, 1, 15), result.date());
  }

  @Test
  void updateProjectTimeslot_shouldUpdateAndRefreshMetrics() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    TimeslotEntity slot = new TimeslotEntity();
    slot.setId("slot-1");
    slot.setTitle("Old Title");
    slot.setDurationMinutes(60);
    slot.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndId(project, "slot-1")).thenReturn(Optional.of(slot));
    when(timeslotRepository.save(any(TimeslotEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TimeslotCreateRequest request =
        new TimeslotCreateRequest(
            "New Title", "updated", 90, LocalDate.of(2026, 1, 15), LocalTime.of(15, 0));

    TimeslotResponse result = service.updateProjectTimeslot("proj-1", "slot-1", request);

    assertEquals("New Title", result.title());
    assertEquals(90, result.durationMinutes());
  }

  @Test
  void deleteProjectTimeslot_shouldRemoveTimeslotAndRefreshMetrics() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    TimeslotEntity slot = new TimeslotEntity();
    slot.setId("slot-1");
    slot.setProject(project);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndId(project, "slot-1")).thenReturn(Optional.of(slot));

    service.deleteProjectTimeslot("proj-1", "slot-1");

    verify(timeslotRepository).delete(slot);
  }

  @Test
  void createProjectStudent_shouldThrowWhenProjectNotFound() {
    when(projectRepository.findBySlug("missing")).thenReturn(Optional.empty());
    StudentCreateRequest request = new StudentCreateRequest("Alice", "", null);

    assertThrows(
        ProjectNotFoundException.class, () -> service.createProjectStudent("missing", request));
  }

  @Test
  void deleteProject_shouldThrowWhenProjectNotFound() {
    when(projectRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(ProjectNotFoundException.class, () -> service.deleteProject("missing"));
  }

  @Test
  void getProjectTimeslots_shouldReturnTimeslotsInMonthWindow() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");

    TimeslotEntity slot1 = new TimeslotEntity();
    slot1.setId("slot-1");
    slot1.setTitle("Session 1");
    slot1.setDate(LocalDate.of(2026, 1, 10));
    slot1.setProject(project);

    TimeslotEntity slot2 = new TimeslotEntity();
    slot2.setId("slot-2");
    slot2.setTitle("Session 2");
    slot2.setDate(LocalDate.of(2026, 1, 20));
    slot2.setProject(project);

    YearMonth month = YearMonth.parse("2026-01");
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to))
        .thenReturn(List.of(slot1, slot2));

    List<TimeslotResponse> results = service.getProjectTimeslots("proj-1", "2026-01");

    assertEquals(2, results.size());
  }
}
