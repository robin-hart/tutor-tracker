package com.tutortimetracker.api.config;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.ProjectGroupEntity;
import com.tutortimetracker.api.entity.ReportRowEntity;
import com.tutortimetracker.api.entity.StudentEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.ProjectGroupRepository;
import com.tutortimetracker.api.repository.ReportRepository;
import com.tutortimetracker.api.repository.StudentRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds baseline mockup data into MariaDB on first boot.
 */
@Component
@Profile("!test")
public class DemoDataSeeder {

    private static final String GROUP_UNGROUPED = "Ungrouped";
    private static final String GROUP_A = "Group A";
    private static final String GROUP_B = "Group B";

    private final ProjectRepository projectRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final StudentRepository studentRepository;
    private final TimeslotRepository timeslotRepository;
    private final ReportRepository reportRepository;

    /**
     * @param projectRepository project persistence dependency
    * @param projectGroupRepository group persistence dependency
     * @param studentRepository student persistence dependency
     * @param timeslotRepository timeslot persistence dependency
     * @param reportRepository report persistence dependency
     */
    public DemoDataSeeder(
            ProjectRepository projectRepository,
            ProjectGroupRepository projectGroupRepository,
            StudentRepository studentRepository,
            TimeslotRepository timeslotRepository,
            ReportRepository reportRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.studentRepository = studentRepository;
        this.timeslotRepository = timeslotRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Inserts initial records only when the database is empty.
     */
    @PostConstruct
    public void seed() {
        if (projectRepository.count() > 0) {
            return;
        }

        ProjectEntity math = buildProject("math-grade-10", "Math Grade 10", "STEM", 48.0, 12.5, 75);
        ProjectEntity physics = buildProject("physics-university", "Physics University", "SCIENCE", 32.2, 8.0, 50);
        ProjectEntity sat = buildProject("sat-verbal-prep", "SAT Verbal Prep", "EXAM PREP", 15.5, 15.5, 25);

        List<ProjectEntity> projects = projectRepository.saveAll(List.of(math, physics, sat));
        ProjectEntity persistedMath = projects.stream().filter(project -> "math-grade-10".equals(project.getSlug())).findFirst().orElseThrow();

        projectGroupRepository.saveAll(List.of(
            buildGroup(GROUP_UNGROUPED, persistedMath),
            buildGroup(GROUP_A, persistedMath),
            buildGroup(GROUP_B, persistedMath)
        ));

        studentRepository.saveAll(List.of(
            buildStudent("alex-thompson", "Alex Thompson", "Struggling with quadratic equations. Requires focus on discriminant formula next session.", GROUP_A, persistedMath),
            buildStudent("maya-rodriguez", "Maya Rodriguez", "Excellent grasp of trigonometry. Ready for advanced circle theorem exercises.", GROUP_A, persistedMath),
            buildStudent("jordan-chen", "Jordan Chen", "Consistent homework completion. Needs more confidence with word problems involving systems.", GROUP_B, persistedMath)
        ));

        timeslotRepository.saveAll(List.of(
            buildTimeslot("slot-1", "Equation Review", "Focusing on quadratic systems and graphing techniques.", 90, LocalDate.now(), LocalTime.of(14, 0), persistedMath),
            buildTimeslot("slot-2", "Homework Audit", "Reviewing Module 4 assignments for Sarah Jenkins.", 75, LocalDate.now(), LocalTime.of(17, 15), persistedMath)
        ));

        reportRepository.saveAll(List.of(
            buildReport("2023-10", "October 2023", "Advanced Calculus Prep", 42.5, 12, 2550.00, persistedMath),
            buildReport("2023-09", "September 2023", "Organic Chemistry Lab", 38.0, 10, 2280.00, persistedMath),
            buildReport("2023-08", "August 2023", "Advanced Calculus Prep", 29.0, 8, 1740.00, persistedMath)
        ));
    }

    private ProjectEntity buildProject(String slug, String name, String category, double totalHours, double monthHours, int completionPercent) {
        ProjectEntity entity = new ProjectEntity();
        entity.setSlug(slug);
        entity.setName(name);
        entity.setCategory(category);
        entity.setTotalHours(totalHours);
        entity.setMonthHours(monthHours);
        entity.setCompletionPercent(completionPercent);
        return entity;
    }

    private StudentEntity buildStudent(String studentKey, String name, String notes, String groupName, ProjectEntity project) {
        StudentEntity entity = new StudentEntity();
        entity.setStudentKey(studentKey);
        entity.setName(name);
        entity.setNotes(notes);
        entity.setGroupName(groupName);
        entity.setProject(project);
        return entity;
    }

    private TimeslotEntity buildTimeslot(String id, String title, String description, int durationMinutes, LocalDate date, LocalTime startTime, ProjectEntity project) {
        TimeslotEntity entity = new TimeslotEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setDurationMinutes(durationMinutes);
        entity.setDate(date);
        entity.setStartTime(startTime);
        entity.setProject(project);
        return entity;
    }

    private ProjectGroupEntity buildGroup(String name, ProjectEntity project) {
        ProjectGroupEntity entity = new ProjectGroupEntity();
        entity.setName(name);
        entity.setProject(project);
        return entity;
    }

    private ReportRowEntity buildReport(String monthKey, String month, String projectName, double totalHours, int sessions, double grossAmount, ProjectEntity project) {
        ReportRowEntity entity = new ReportRowEntity();
        entity.setMonthKey(monthKey);
        entity.setMonth(month);
        entity.setProjectName(projectName);
        entity.setTotalHours(totalHours);
        entity.setSessions(sessions);
        entity.setGrossAmount(grossAmount);
        entity.setProject(project);
        return entity;
    }
}
