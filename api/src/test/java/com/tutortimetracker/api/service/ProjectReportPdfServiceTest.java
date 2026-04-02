package com.tutortimetracker.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    service = new ProjectReportPdfService(projectRepository, timeslotRepository, latexCompiler);
  }

  @Test
  void exportProjectMonthPdf_shouldRenderMonthSlotsIntoLatexAndReturnPdf() {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");
    project.setName("Math & Science");
    project.setInstitution("University Lab");
    project.setTargetMonthHours(12.5);
    project.setCreatedAt(LocalDate.of(2026, 1, 1));

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
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(
            eq(project), eq(LocalDate.of(2026, 1, 1)), eq(from)))
        .thenReturn(List.of());
    when(latexCompiler.compileToPdf(any(String.class))).thenReturn(expectedPdf);

    byte[] actual = service.exportProjectMonthPdf("proj-1", "2026-03");

    assertArrayEquals(expectedPdf, actual);

    ArgumentCaptor<String> latexCaptor = ArgumentCaptor.forClass(String.class);
    verify(latexCompiler).compileToPdf(latexCaptor.capture());

    String latex = latexCaptor.getValue();
    assertTrue(latex.contains("Arbeitszeitblatt"));
    assertTrue(latex.contains("Math"));
    assertTrue(latex.contains("Science"));
    assertTrue(latex.contains("University Lab"));
    assertTrue(latex.contains("Session"));
    assertTrue(latex.contains("12.03.2026"));
    assertTrue(latex.contains("14:30"));
    assertTrue(latex.contains("16:00"));
    assertTrue(latex.contains("1h 30min"));
    assertTrue(latex.contains("Unterschrift"));
  }

  @Test
  void exportProjectMonthPdf_shouldThrowWhenProjectMissing() {
    when(projectRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ProjectNotFoundException.class, () -> service.exportProjectMonthPdf("missing", "2026-03"));
  }

  @ParameterizedTest(name = "carryover calculation for {0}")
  @MethodSource("carryoverCases")
  void exportProjectMonthPdf_shouldCalculateCarryoverAndTargetHours(
      List<Integer> previousDurations,
      List<Integer> currentDurations,
      double targetMonthHours,
      String expectedPreviousCarryover,
      String expectedNextCarryover) {
    ProjectEntity project = new ProjectEntity();
    project.setSlug("proj-1");
    project.setName("Math");
    project.setInstitution("University Lab");
    project.setTargetMonthHours(targetMonthHours);
    project.setCreatedAt(LocalDate.of(2026, 3, 1));

    YearMonth month = YearMonth.parse("2026-04");
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    List<TimeslotEntity> previousSlots = buildSlots(previousDurations, LocalDate.of(2026, 3, 5));
    List<TimeslotEntity> currentSlots = buildSlots(currentDurations, LocalDate.of(2026, 4, 10));

    when(projectRepository.findBySlug("proj-1")).thenReturn(Optional.of(project));
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to))
        .thenReturn(currentSlots);
    when(timeslotRepository.findByProjectAndDateGreaterThanEqualAndDateLessThan(
            eq(project), eq(LocalDate.of(2026, 3, 1)), eq(from)))
        .thenReturn(previousSlots);
    when(latexCompiler.compileToPdf(any(String.class))).thenReturn("pdf-content".getBytes());

    byte[] actual = service.exportProjectMonthPdf("proj-1", "2026-04");

    assertArrayEquals("pdf-content".getBytes(), actual);

    ArgumentCaptor<String> latexCaptor = ArgumentCaptor.forClass(String.class);
    verify(latexCompiler).compileToPdf(latexCaptor.capture());

    String latex = latexCaptor.getValue();
    assertTrue(latex.contains("IST-Arbeitszeit des Abrechnungsmonats:"));
    assertTrue(latex.contains("SOLL-Arbeitszeit des Abrechnungsmonats:"));
    assertTrue(latex.contains("Summe (IST+Übertrag):"));
    assertTrue(latex.contains(expectedPreviousCarryover));
    assertTrue(latex.contains(expectedNextCarryover));
  }

  private static Stream<Arguments> carryoverCases() {
    return Stream.of(
        Arguments.of(List.of(), List.of(), 0.0, "0h 00min", "0h 00min"),
        Arguments.of(List.of(), List.of(120, 120), 8.0, "-8h 00min", "-12h 00min"),
        Arguments.of(List.of(), List.of(360), 6.0, "-6h 00min", "-6h 00min"),
        Arguments.of(List.of(120), List.of(120), 6.0, "-4h 00min", "-8h 00min"),
        Arguments.of(List.of(480), List.of(0), 6.0, "2h 00min", "-4h 00min"),
        Arguments.of(List.of(180, 120), List.of(60), 5.0, "0h 00min", "-4h 00min"),
        Arguments.of(List.of(300), List.of(240), 9.0, "-4h 00min", "-9h 00min"));
  }

  private static List<TimeslotEntity> buildSlots(List<Integer> durations, LocalDate dateStart) {
    List<TimeslotEntity> slots = new ArrayList<>();
    for (int index = 0; index < durations.size(); index++) {
      TimeslotEntity slot = new TimeslotEntity();
      slot.setTitle("Slot " + (index + 1));
      slot.setDate(dateStart.plusDays(index));
      slot.setStartTime(LocalTime.of(8, 0));
      slot.setDurationMinutes(durations.get(index));
      slots.add(slot);
    }
    return slots;
  }
}
