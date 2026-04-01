package com.tutortimetracker.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
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

@ExtendWith(MockitoExtension.class)
class ProjectReportPdfServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private TimeslotRepository timeslotRepository;
  @Mock private LatexCompiler latexCompiler;

  private ProjectReportPdfService service;

  @BeforeEach
  void setUp() {
    service =
        new ProjectReportPdfService(
            projectRepository,
            timeslotRepository,
            latexCompiler,
            "templates/project-monthly-report.tpl");
  }

  @Test
  void exportProjectMonthPdf_shouldRenderMonthSlotsIntoLatexAndReturnPdf() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");
    project.setName("Math & Science");

    TimeslotEntity slot = new TimeslotEntity();
    slot.setTitle("Session #1");
    slot.setDate(LocalDate.of(2026, 3, 12));
    slot.setStartTime(LocalTime.of(14, 30));
    slot.setDurationMinutes(90);

    YearMonth month = YearMonth.parse("2026-03");
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    byte[] expectedPdf = "pdf-content".getBytes();

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to))
        .thenReturn(List.of(slot));
    when(latexCompiler.compileToPdf(any(String.class))).thenReturn(expectedPdf);

    byte[] actual = service.exportProjectMonthPdf("proj-1", "2026-03");

    assertArrayEquals(expectedPdf, actual);

    ArgumentCaptor<String> latexCaptor = ArgumentCaptor.forClass(String.class);
    verify(latexCompiler).compileToPdf(latexCaptor.capture());

    String latex = latexCaptor.getValue();
    assertTrue(latex.contains("Math"));
    assertTrue(latex.contains("Science"));
    assertTrue(latex.contains("Session"));
    assertTrue(latex.contains("2026-03-12"));
    assertTrue(latex.contains("14:30"));
    assertTrue(latex.contains("1.50"));
    assertTrue(latex.contains("90.00"));
  }

  @Test
  void exportProjectMonthPdf_shouldThrowWhenProjectMissing() {
    when(projectRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ProjectNotFoundException.class, () -> service.exportProjectMonthPdf("missing", "2026-03"));
  }
}
