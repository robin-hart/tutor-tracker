package com.tutortimetracker.api.service;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.ReportRowEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.entity.TodaySlotEntity;
import com.tutortimetracker.api.model.CalendarSlot;
import com.tutortimetracker.api.model.ProjectCalendarResponse;
import com.tutortimetracker.api.model.ProjectCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupCreateRequest;
import com.tutortimetracker.api.model.ProjectGroupSummary;
import com.tutortimetracker.api.model.ProjectSummary;
import com.tutortimetracker.api.model.ReportRow;
import com.tutortimetracker.api.model.StudentCreateRequest;
import com.tutortimetracker.api.model.StudentGroupUpdateRequest;
import com.tutortimetracker.api.model.StudentNotesUpdateRequest;
import com.tutortimetracker.api.model.StudentProfile;
import com.tutortimetracker.api.model.TimeslotCreateRequest;
import com.tutortimetracker.api.model.TimeslotResponse;
import com.tutortimetracker.api.model.TodaySlot;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.ReportRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import com.tutortimetracker.api.repository.TodaySlotRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Core business logic service for TutorTimeTracker data operations.
 *
 * <p>This service manages all domain operations including project lifecycle management, student
 * record tracking, timeslot scheduling, group management, and monthly report generation. The
 * service implements transactional consistency and enforces business rules such as:
 *
 * <ul>
 *   <li>Automatic project slug generation with uniqueness constraints
 *   <li>Default "Ungrouped" group creation for all projects
 *   <li>String trimming and normalization for all user inputs
 *   <li>Cascading deletion of dependent entities (students, timeslots, reports)
 *   <li>Student group reassignment to "Ungrouped" when groups are deleted
 * </ul>
 *
 * <h2>Data Flow</h2>
 *
 * <p>Controller → Service (business logic) → Repository (persistence) → Database
 *
 * <h2>Key Responsibilities</h2>
 *
 * <table border="1" style="padding: 5px;">
 *   <caption>Entity Operations and Business Rules</caption>
 *   <tr>
 *     <th>Entity</th>
 *     <th>Operations</th>
 *     <th>Business Rules</th>
 *   </tr>
 *   <tr>
 *     <td>Project</td>
 *     <td>Create, Read, Delete, Metrics Refresh</td>
 *     <td>Auto-slug, default group creation, cascading delete</td>
 *   </tr>
 *   <tr>
 *     <td>Student</td>
 *     <td>Create, Read, Update Notes, Update Group, Delete</td>
 *     <td>Notes trimming, default "Ungrouped" assignment</td>
 *   </tr>
 *   <tr>
 *     <td>Group</td>
 *     <td>Create, Read, Delete</td>
 *     <td>Cannot delete "Ungrouped", auto-reassign students</td>
 *   </tr>
 *   <tr>
 *     <td>Timeslot</td>
 *     <td>Create, Read, Update, Delete, Metrics Refresh</td>
 *     <td>Duration tracking, project metrics aggregation</td>
 *   </tr>
 *   <tr>
 *     <td>Report</td>
 *     <td>Generate, Read</td>
 *     <td>Monthly aggregation, gross amount calculation (rate: $60/hr)</td>
 *   </tr>
 * </table>
 *
 * @see com.tutortimetracker.api.entity
 * @see com.tutortimetracker.api.repository
 * @author TutorTimeTracker Team
 * @version 1.0
 */
@Service
public class TutorDataService {

  private static final double DEFAULT_HOURLY_RATE = 60.0;
  private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
  private static final String UNGROUPED_GROUP = "Ungrouped";
  private static final String STUDENT_NOT_FOUND_PREFIX = "Student not found: ";

  private final ProjectRepository projectRepository;
  private final ProjectGroupRepository projectGroupRepository;
  private final StudentRepository studentRepository;
  private final ReportRepository reportRepository;
  private final TimeslotRepository timeslotRepository;
  private final TodaySlotRepository todaySlotRepository;

  /**
   * @param projectRepository project persistence dependency
   * @param projectGroupRepository project-group persistence dependency
   * @param studentRepository student persistence dependency
   * @param reportRepository report persistence dependency
   * @param timeslotRepository timeslot persistence dependency
   * @param todaySlotRepository today-slot persistence dependency
   */
  public TutorDataService(
      ProjectRepository projectRepository,
      ProjectGroupRepository projectGroupRepository,
      StudentRepository studentRepository,
      ReportRepository reportRepository,
      TimeslotRepository timeslotRepository,
      TodaySlotRepository todaySlotRepository) {
    this.projectRepository = projectRepository;
    this.projectGroupRepository = projectGroupRepository;
    this.studentRepository = studentRepository;
    this.reportRepository = reportRepository;
    this.timeslotRepository = timeslotRepository;
    this.todaySlotRepository = todaySlotRepository;
  }

  /**
   * @return project summaries for the dashboard
   */
  public List<ProjectSummary> getProjects() {
    return projectRepository.findAll().stream()
        .map(project -> {
          refreshProjectMetrics(project);
          return toProjectSummary(project);
        })
        .toList();
  }

  /**
   * Creates a project.
   *
   * @param request incoming create request
   * @return created project summary
   */
  public ProjectSummary createProject(ProjectCreateRequest request) {
    String normalizedName = request.name().trim();
    String normalizedCategory = request.category().trim();
    String baseSlug = slugify(normalizedName);

    for (int attempt = 0; attempt < 5; attempt++) {
      String candidateSlug = generateProjectSlugCandidate(baseSlug, attempt);

      ProjectEntity entity = new ProjectEntity();
      entity.setSlug(candidateSlug);
      entity.setName(normalizedName);
      entity.setCategory(normalizedCategory);
      entity.setTotalHours(request.totalHours());
      entity.setMonthHours(request.monthHours());
      entity.setCompletionPercent(request.completionPercent());

      try {
        ProjectEntity created = projectRepository.save(entity);
        ensureDefaultGroup(created);
        return toProjectSummary(created);
      } catch (DataIntegrityViolationException ex) {
        if (!isDuplicateSlugInsert(ex, candidateSlug)) {
          throw ex;
        }
      }
    }

    throw new IllegalStateException("Could not create project due to repeated slug collisions.");
  }

  /**
   * Deletes a project and all associated data (students, timeslots, groups, reports, today slots).
   *
   * @param projectId project slug
   */
  public void deleteProject(String projectId) {
    ProjectEntity project = findProjectBySlug(projectId);

    // Delete all students in the project
    List<StudentEntity> students = studentRepository.findByProject(project);
    studentRepository.deleteAll(students);

    // Delete all timeslots in the project
    List<TimeslotEntity> timeslots = timeslotRepository.findByProject(project);
    timeslotRepository.deleteAll(timeslots);

    // Delete all today slots in the project
    List<TodaySlotEntity> todaySlots = todaySlotRepository.findByProject(project);
    todaySlotRepository.deleteAll(todaySlots);

    // Delete all groups in the project
    List<ProjectGroupEntity> groups = projectGroupRepository.findByProject(project);
    projectGroupRepository.deleteAll(groups);

    // Delete all reports for the project
    List<ReportRowEntity> reports = reportRepository.findByProject(project);
    reportRepository.deleteAll(reports);

    // Delete the project itself
    projectRepository.delete(project);
  }

  /**
   * @param projectId selected project id
   * @return calendar information and today's slots
   */
  public ProjectCalendarResponse getProjectCalendar(String projectId, String monthKey) {
    ProjectEntity project = findProjectBySlug(projectId);
    refreshProjectMetrics(project);
    YearMonth month = parseMonthKeyOrNow(monthKey);
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    List<TimeslotEntity> monthSlots =
        timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to);
    List<TimeslotEntity> allSlots = timeslotRepository.findByProject(project);
    List<TodaySlot> todaySlots =
        timeslotRepository.findByProjectAndDate(project, LocalDate.now()).stream()
            .map(
                slot ->
                    new TodaySlot(
                        slot.getTitle(),
                        slot.getStartTime().toString(),
                        slot.getDescription() == null ? "" : slot.getDescription()))
            .toList();

    return new ProjectCalendarResponse(
        project.getSlug(),
        project.getName(),
        project.getTotalHours(),
        project.getMonthHours(),
        todaySlots,
        monthSlots.stream()
            .map(
                slot ->
                    new CalendarSlot(
                        slot.getId(),
                        slot.getTitle(),
                        slot.getDate().toString(),
                        slot.getStartTime().toString(),
                        slot.getDurationMinutes()))
            .toList(),
        allSlots.stream()
            .map(
                slot ->
                    new CalendarSlot(
                        slot.getId(),
                        slot.getTitle(),
                        slot.getDate().toString(),
                        slot.getStartTime().toString(),
                        slot.getDurationMinutes()))
            .toList());
  }

  /**
   * @param projectId selected project id
   * @return students linked to the project
   */
  public List<StudentProfile> getProjectStudents(String projectId) {
    ProjectEntity project = findProjectBySlug(projectId);
    ensureDefaultGroup(project);
    return studentRepository.findByProject(project).stream()
        .map(
            student ->
                new StudentProfile(
                    student.getStudentKey(),
                    student.getName(),
                    student.getNotes(),
                    normalizeGroupName(student.getGroupName())))
        .toList();
  }

  /**
   * Returns available student groups for one project.
   *
   * @param projectId project slug
   * @return group summaries with student counts
   */
  public List<ProjectGroupSummary> getProjectGroups(String projectId) {
    ProjectEntity project = findProjectBySlug(projectId);
    ensureDefaultGroup(project);

    List<StudentEntity> projectStudents = studentRepository.findByProject(project);
    for (StudentEntity student : projectStudents) {
      String existingGroupName = normalizeGroupName(student.getGroupName());
      if (projectGroupRepository.findByProjectAndName(project, existingGroupName).isEmpty()) {
        ProjectGroupEntity group = new ProjectGroupEntity();
        group.setName(existingGroupName);
        group.setProject(project);
        projectGroupRepository.save(group);
      }
    }

    List<ProjectGroupEntity> groups = projectGroupRepository.findByProject(project);
    Map<String, Integer> counts =
        projectStudents.stream()
            .collect(
                Collectors.toMap(
                    student -> normalizeGroupName(student.getGroupName()),
                    student -> 1,
                    Integer::sum));

    return groups.stream()
        .map(
            group ->
                new ProjectGroupSummary(group.getName(), counts.getOrDefault(group.getName(), 0)))
        .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
        .toList();
  }

  /**
   * Creates a group in a project.
   *
   * @param projectId project slug
   * @param request create group payload
   * @return created or existing group summary
   */
  public ProjectGroupSummary createProjectGroup(
      String projectId, ProjectGroupCreateRequest request) {
    ProjectEntity project = findProjectBySlug(projectId);
    String groupName = normalizeGroupName(request.name());

    if (projectGroupRepository.findByProjectAndName(project, groupName).isEmpty()) {
      ProjectGroupEntity group = new ProjectGroupEntity();
      group.setName(groupName);
      group.setProject(project);
      projectGroupRepository.save(group);
    }

    int count = studentRepository.findByProjectAndGroupName(project, groupName).size();
    return new ProjectGroupSummary(groupName, count);
  }

  /**
   * Deletes a project group and reassigns students to Ungrouped.
   *
   * @param projectId project slug
   * @param rawGroupName group route value
   */
  public void deleteProjectGroup(String projectId, String rawGroupName) {
    ProjectEntity project = findProjectBySlug(projectId);
    String groupName = normalizeGroupName(rawGroupName);

    if (UNGROUPED_GROUP.equalsIgnoreCase(groupName)) {
      throw new IllegalArgumentException("Ungrouped cannot be deleted.");
    }

    ProjectGroupEntity group =
        projectGroupRepository
            .findByProjectAndName(project, groupName)
            .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupName));

    List<StudentEntity> assignedStudents =
        studentRepository.findByProjectAndGroupName(project, groupName);
    for (StudentEntity student : assignedStudents) {
      student.setGroupName(UNGROUPED_GROUP);
    }
    studentRepository.saveAll(assignedStudents);

    projectGroupRepository.delete(group);
    ensureDefaultGroup(project);
  }

  /**
   * Returns project-scoped timeslots in a month window.
   *
   * @param projectId project slug
   * @param monthKey optional month key in yyyy-MM format
   * @return slots in the selected month
   */
  public List<TimeslotResponse> getProjectTimeslots(String projectId, String monthKey) {
    ProjectEntity project = findProjectBySlug(projectId);
    YearMonth month = parseMonthKeyOrNow(monthKey);
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);
    return timeslotRepository
        .findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to)
        .stream()
        .map(slot -> toTimeslotResponse(slot, projectId))
        .toList();
  }

  /**
   * Creates a student for the selected project.
   *
   * @param projectId target project slug
   * @param request create student request
   * @return created student profile
   */
  public StudentProfile createProjectStudent(String projectId, StudentCreateRequest request) {
    ProjectEntity project = findProjectBySlug(projectId);
    String groupName = normalizeGroupName(request.groupName());
    ensureGroupExists(project, groupName);

    StudentEntity entity = new StudentEntity();
    entity.setStudentKey(UUID.randomUUID().toString());
    entity.setName(request.name().trim());
    entity.setNotes(request.notes() != null ? request.notes().trim() : "");
    entity.setGroupName(groupName);
    entity.setProject(project);

    StudentEntity created = studentRepository.save(entity);
    return new StudentProfile(
        created.getStudentKey(),
        created.getName(),
        created.getNotes(),
        normalizeGroupName(created.getGroupName()));
  }

  /**
   * Updates notes for an existing student.
   *
   * @param studentId student key
   * @param request notes update request
   * @return updated student profile
   */
  public StudentProfile updateStudentNotes(String studentId, StudentNotesUpdateRequest request) {
    StudentEntity student =
        studentRepository
            .findByStudentKey(studentId)
            .orElseThrow(() -> new StudentNotFoundException(STUDENT_NOT_FOUND_PREFIX + studentId));

    student.setNotes(request.notes().trim());
    StudentEntity updated = studentRepository.save(student);

    return new StudentProfile(
        updated.getStudentKey(),
        updated.getName(),
        updated.getNotes(),
        normalizeGroupName(updated.getGroupName()));
  }

  /**
   * Updates the group assignment for an existing student.
   *
   * @param studentId student key
   * @param request group update request
   * @return updated student profile
   */
  public StudentProfile updateStudentGroup(String studentId, StudentGroupUpdateRequest request) {
    StudentEntity student =
        studentRepository
            .findByStudentKey(studentId)
            .orElseThrow(() -> new StudentNotFoundException(STUDENT_NOT_FOUND_PREFIX + studentId));
    ProjectEntity project = student.getProject();
    String groupName = normalizeGroupName(request.groupName());
    ensureGroupExists(project, groupName);

    student.setGroupName(groupName);
    StudentEntity updated = studentRepository.save(student);

    return new StudentProfile(
        updated.getStudentKey(),
        updated.getName(),
        updated.getNotes(),
        normalizeGroupName(updated.getGroupName()));
  }

  /**
   * Deletes a student.
   *
   * @param studentId student key
   */
  public void deleteStudent(String studentId) {
    StudentEntity student =
        studentRepository
            .findByStudentKey(studentId)
            .orElseThrow(() -> new StudentNotFoundException(STUDENT_NOT_FOUND_PREFIX + studentId));
    studentRepository.delete(student);
  }

  /**
   * @return report rows for the export table
   */
  public List<ReportRow> getReports() {
    return reportRepository.findAll().stream().map(this::toReportRow).toList();
  }

  /**
   * Returns monthly reports for a project.
   *
   * @param projectId project slug
   * @return reports for the project
   */
  public List<ReportRow> getProjectReports(String projectId) {
    ProjectEntity project = findProjectBySlug(projectId);
    return reportRepository.findByProject(project).stream().map(this::toReportRow).toList();
  }

  /**
   * Generates or refreshes a report for a project and month from timeslots.
   *
   * @param projectId project slug
   * @param monthKey month key in yyyy-MM format
   * @return generated report row
   */
  public ReportRow generateProjectMonthlyReport(String projectId, String monthKey) {
    ProjectEntity project = findProjectBySlug(projectId);
    YearMonth month = parseMonthKeyOrNow(monthKey);
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    List<TimeslotEntity> slots =
        timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to);
    int totalMinutes = slots.stream().mapToInt(TimeslotEntity::getDurationMinutes).sum();
    double hours = totalMinutes / 60.0;
    int sessions = slots.size();
    double grossAmount = hours * DEFAULT_HOURLY_RATE;

    ReportRowEntity report =
        reportRepository
            .findByProjectAndMonthKey(project, month.toString())
            .orElseGet(ReportRowEntity::new);

    report.setProject(project);
    report.setMonthKey(month.toString());
    report.setMonth(month.format(MONTH_LABEL_FORMATTER));
    report.setProjectName(project.getName());
    report.setTotalHours(hours);
    report.setSessions(sessions);
    report.setGrossAmount(grossAmount);

    return toReportRow(reportRepository.save(report));
  }

  /**
   * Creates a new timeslot object.
   *
   * @param request incoming create request
   * @return created timeslot representation
   */
  public TimeslotResponse createProjectTimeslot(String projectId, TimeslotCreateRequest request) {
    ProjectEntity project = findProjectBySlug(projectId);
    TimeslotEntity entity = new TimeslotEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setTitle(request.title());
    entity.setDescription(request.description());
    entity.setDurationMinutes(request.durationMinutes());
    entity.setDate(request.date());
    entity.setStartTime(request.startTime());
    entity.setProject(project);

    TimeslotEntity created = timeslotRepository.save(entity);
    refreshProjectMetrics(project);

    return toTimeslotResponse(created, project.getSlug());
  }

  /**
   * Gets one timeslot in a project calendar.
   *
   * @param projectId project slug
   * @param timeslotId timeslot id
   * @return timeslot payload
   */
  public TimeslotResponse getProjectTimeslot(String projectId, String timeslotId) {
    ProjectEntity project = findProjectBySlug(projectId);
    TimeslotEntity slot = findTimeslot(project, timeslotId);
    return toTimeslotResponse(slot, project.getSlug());
  }

  /**
   * Updates one timeslot in a project calendar.
   *
   * @param projectId project slug
   * @param timeslotId timeslot id
   * @param request update payload
   * @return updated timeslot
   */
  public TimeslotResponse updateProjectTimeslot(
      String projectId, String timeslotId, TimeslotCreateRequest request) {
    ProjectEntity project = findProjectBySlug(projectId);
    TimeslotEntity slot = findTimeslot(project, timeslotId);

    slot.setTitle(request.title());
    slot.setDescription(request.description());
    slot.setDurationMinutes(request.durationMinutes());
    slot.setDate(request.date());
    slot.setStartTime(request.startTime());

    TimeslotEntity updated = timeslotRepository.save(slot);
    refreshProjectMetrics(project);
    return toTimeslotResponse(updated, project.getSlug());
  }

  /**
   * Deletes one timeslot from a project calendar.
   *
   * @param projectId project slug
   * @param timeslotId timeslot id
   */
  public void deleteProjectTimeslot(String projectId, String timeslotId) {
    ProjectEntity project = findProjectBySlug(projectId);
    TimeslotEntity slot = findTimeslot(project, timeslotId);
    timeslotRepository.delete(slot);
    refreshProjectMetrics(project);
  }

  /**
   * Legacy endpoint support: creates timeslot under default project.
   *
   * @param request incoming create request
   * @return created timeslot
   */
  public TimeslotResponse createTimeslot(TimeslotCreateRequest request) {
    return createProjectTimeslot("math-grade-10", request);
  }

  /**
   * Resolves a project by its slug.
   *
   * @param projectId slug from route parameter
   * @return matching project entity
   */
  private ProjectEntity findProjectBySlug(String projectId) {
    return projectRepository
        .findBySlug(projectId)
        .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
  }

  /**
   * Resolves one timeslot within a project scope.
   *
   * @param project owning project
   * @param timeslotId timeslot id
   * @return matching timeslot
   */
  private TimeslotEntity findTimeslot(ProjectEntity project, String timeslotId) {
    return timeslotRepository
        .findByProjectAndId(project, timeslotId)
        .orElseThrow(() -> new TimeslotNotFoundException("Timeslot not found: " + timeslotId));
  }

  /**
   * Updates project hour metrics based on timeslot data.
   *
   * @param project project to refresh
   */
  private void refreshProjectMetrics(ProjectEntity project) {
    LocalDate now = LocalDate.now();
    YearMonth current = YearMonth.from(now);
    LocalDate from = current.atDay(1);
    LocalDate to = current.plusMonths(1).atDay(1);

    double totalHours =
        timeslotRepository
                .findByProjectAndDateGreaterThanEqualAndDateLessThan(
                    project, LocalDate.of(1970, 1, 1), LocalDate.of(2999, 1, 1))
                .stream()
                .mapToInt(TimeslotEntity::getDurationMinutes)
                .sum()
            / 60.0;

    double monthHours =
        timeslotRepository
                .findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to)
                .stream()
                .mapToInt(TimeslotEntity::getDurationMinutes)
                .sum()
            / 60.0;

    project.setTotalHours(totalHours);
    project.setMonthHours(monthHours);
    projectRepository.save(project);
  }

  /**
   * Maps project entity to API model.
   *
   * @param project project entity
   * @return API summary
   */
  private ProjectSummary toProjectSummary(ProjectEntity project) {
    return new ProjectSummary(
        project.getSlug(),
        project.getName(),
        project.getCategory(),
        project.getTotalHours(),
        project.getMonthHours(),
        project.getCompletionPercent(),
        project.getCreatedAt());
  }

  /**
   * Maps report entity to API model.
   *
   * @param report report entity
   * @return API row
   */
  private ReportRow toReportRow(ReportRowEntity report) {
    String projectId = report.getProject() == null ? null : report.getProject().getSlug();
    return new ReportRow(
        projectId,
        report.getMonth(),
        report.getProjectName(),
        report.getTotalHours(),
        report.getSessions(),
        report.getGrossAmount());
  }

  /**
   * Maps timeslot entity to API model.
   *
   * @param entity timeslot entity
   * @param projectId owning project id
   * @return API payload
   */
  private TimeslotResponse toTimeslotResponse(TimeslotEntity entity, String projectId) {
    return new TimeslotResponse(
        entity.getId(),
        projectId,
        entity.getTitle(),
        entity.getDescription(),
        entity.getDurationMinutes(),
        entity.getDate(),
        entity.getStartTime());
  }

  /**
   * Parses month key or defaults to current month.
   *
   * @param monthKey optional month key
   * @return parsed month
   */
  private YearMonth parseMonthKeyOrNow(String monthKey) {
    if (monthKey == null || monthKey.isBlank()) {
      return YearMonth.now();
    }
    return YearMonth.parse(monthKey);
  }

  /**
   * Converts a label into a URL-safe slug.
   *
   * @param rawName project name
   * @return slug candidate
   */
  private String slugify(String rawName) {
    return rawName
        .trim()
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+", "")
        .replaceAll("-+$", "");
  }

  /**
   * Normalizes optional group names to a stable default value.
   *
   * @param groupName optional group name from API payload or persistence
   * @return non-empty group label
   */
  private String normalizeGroupName(String groupName) {
    if (groupName == null || groupName.isBlank()) {
      return UNGROUPED_GROUP;
    }
    return groupName.trim();
  }

  /**
   * Ensures the default Ungrouped group exists for the given project.
   *
   * @param project project context
   */
  private void ensureDefaultGroup(ProjectEntity project) {
    if (projectGroupRepository.findByProjectAndName(project, UNGROUPED_GROUP).isEmpty()) {
      ProjectGroupEntity group = new ProjectGroupEntity();
      group.setName(UNGROUPED_GROUP);
      group.setProject(project);
      projectGroupRepository.save(group);
    }
  }

  /**
   * Verifies that a target group exists in the given project.
   *
   * @param project project context
   * @param groupName desired group
   */
  private void ensureGroupExists(ProjectEntity project, String groupName) {
    if (UNGROUPED_GROUP.equalsIgnoreCase(groupName)) {
      ensureDefaultGroup(project);
      return;
    }

    if (projectGroupRepository.findByProjectAndName(project, groupName).isEmpty()) {
      throw new IllegalArgumentException("Group not found in project: " + groupName);
    }
  }

  /**
   * Ensures project slug uniqueness by appending a numeric suffix.
   *
   * @param baseSlug base slug candidate
   * @return unique slug
   */
  private String ensureUniqueProjectSlug(String baseSlug) {
    String normalizedBase = baseSlug.isBlank() ? "project" : baseSlug;
    String candidate = normalizedBase;
    int suffix = 1;
    while (projectRepository.findBySlug(candidate).isPresent()) {
      suffix++;
      candidate = normalizedBase + "-" + suffix;
    }
    return candidate;
  }

  /**
   * Generates a slug candidate for project creation retries.
   *
   * @param baseSlug raw slug stem
   * @param attempt current retry index
   * @return slug candidate
   */
  private String generateProjectSlugCandidate(String baseSlug, int attempt) {
    if (attempt == 0) {
      return ensureUniqueProjectSlug(baseSlug);
    }

    String normalizedBase = baseSlug.isBlank() ? "project" : baseSlug;
    String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
    return normalizedBase + "-" + randomSuffix;
  }

  /**
   * Detects duplicate-key inserts for project slug writes.
   *
   * @param exception persistence exception
   * @param slugCandidate attempted slug
   * @return true when duplicate key points to the attempted slug
   */
  private boolean isDuplicateSlugInsert(
      DataIntegrityViolationException exception, String slugCandidate) {
    Throwable current = exception;
    while (current != null) {
      String message = current.getMessage();
      if (message != null
          && message.contains("Duplicate entry")
          && message.contains("'" + slugCandidate + "'")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
