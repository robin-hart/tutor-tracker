package com.tutortimetracker.api.service;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Builds a project month report document and compiles it to PDF with LaTeX. */
@Service
public class ProjectReportPdfService {

  private static final double DEFAULT_HOURLY_RATE = 60.0;
  private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter GENERATED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

  private final ProjectRepository projectRepository;
  private final TimeslotRepository timeslotRepository;
  private final LatexCompiler latexCompiler;
  private final String templatePath;

  public ProjectReportPdfService(
      ProjectRepository projectRepository,
      TimeslotRepository timeslotRepository,
      LatexCompiler latexCompiler,
      @Value("${report.export.latex.template:templates/project-monthly-report.tex}")
          String templatePath) {
    this.projectRepository = projectRepository;
    this.timeslotRepository = timeslotRepository;
    this.latexCompiler = latexCompiler;
    this.templatePath = templatePath;
  }

  /**
   * @param projectId project slug
   * @param monthKey month key in yyyy-MM format
   * @return generated PDF bytes
   */
  public byte[] exportProjectMonthPdf(String projectId, String monthKey) {
    ProjectEntity project =
        projectRepository
            .findBySlug(projectId)
            .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

    YearMonth month = parseMonthKeyOrNow(monthKey);
    LocalDate from = month.atDay(1);
    LocalDate to = month.plusMonths(1).atDay(1);

    List<TimeslotEntity> monthSlots =
        timeslotRepository
            .findByProjectAndDateGreaterThanEqualAndDateLessThan(project, from, to)
            .stream()
            .sorted(
                Comparator.comparing(TimeslotEntity::getDate)
                    .thenComparing(TimeslotEntity::getStartTime)
                    .thenComparing(TimeslotEntity::getTitle, String.CASE_INSENSITIVE_ORDER))
            .toList();

    int totalMinutes = monthSlots.stream().mapToInt(TimeslotEntity::getDurationMinutes).sum();
    double totalHours = totalMinutes / 60.0;
    double grossAmount = totalHours * DEFAULT_HOURLY_RATE;

    String template = readTemplate();
    String latex =
        template
            .replace("{{PROJECT_ID}}", escapeLatex(project.getSlug()))
            .replace("{{PROJECT_NAME}}", escapeLatex(project.getName()))
            .replace("{{MONTH_LABEL}}", escapeLatex(month.format(MONTH_LABEL_FORMATTER)))
            .replace("{{SESSION_COUNT}}", Integer.toString(monthSlots.size()))
            .replace("{{TOTAL_HOURS}}", formatDecimal(totalHours))
            .replace("{{HOURLY_RATE}}", formatDecimal(DEFAULT_HOURLY_RATE))
            .replace("{{GROSS_AMOUNT}}", formatDecimal(grossAmount))
            .replace(
                "{{GENERATED_AT}}", escapeLatex(LocalDateTime.now().format(GENERATED_AT_FORMATTER)))
            .replace("{{TIMESLOT_ROWS}}", buildTimeslotRows(monthSlots));

    return latexCompiler.compileToPdf(latex);
  }

  private String readTemplate() {
    ClassPathResource resource = new ClassPathResource(templatePath);
    try {
      return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new PdfReportGenerationException("Could not load LaTeX template: " + templatePath, ex);
    }
  }

  private String buildTimeslotRows(List<TimeslotEntity> monthSlots) {
    if (monthSlots.isEmpty()) {
      return "\\multicolumn{4}{l}{No sessions were scheduled in this month.} \\\\";
    }

    return monthSlots.stream()
        .map(
            slot ->
                String.format(
                    Locale.ENGLISH,
                    "%s & %s & %s & %d \\\\",
                    escapeLatex(slot.getTitle()),
                    slot.getDate().format(DATE_FORMATTER),
                    slot.getStartTime().format(TIME_FORMATTER),
                    slot.getDurationMinutes()))
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
  }

  private String formatDecimal(double value) {
    DecimalFormat format =
        new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    return format.format(value);
  }

  private YearMonth parseMonthKeyOrNow(String monthKey) {
    if (monthKey == null || monthKey.isBlank()) {
      return YearMonth.now();
    }
    return YearMonth.parse(monthKey);
  }

  private String escapeLatex(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }

    StringBuilder escaped = new StringBuilder(raw.length());
    for (char current : raw.toCharArray()) {
      switch (current) {
        case '\\' -> escaped.append("\\\\textbackslash{}");
        case '&' -> escaped.append("\\\\&");
        case '%' -> escaped.append("\\\\%");
        case '$' -> escaped.append("\\\\$");
        case '#' -> escaped.append("\\\\#");
        case '_' -> escaped.append("\\\\_");
        case '{' -> escaped.append("\\\\{");
        case '}' -> escaped.append("\\\\}");
        case '~' -> escaped.append("\\\\textasciitilde{}");
        case '^' -> escaped.append("\\\\textasciicircum{}");
        default -> escaped.append(current);
      }
    }
    return escaped.toString();
  }
}
