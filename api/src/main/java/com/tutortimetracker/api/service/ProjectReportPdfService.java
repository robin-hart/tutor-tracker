package com.tutortimetracker.api.service;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Builds a project month report document and compiles it to PDF with LaTeX. */
@Service
public class ProjectReportPdfService {

  private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("MMMM uuuu", Locale.GERMAN);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.uuuu");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter GENERATED_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.uuuu");
  private static final String LATEX_TEMPLATE =
      """
    \\documentclass[a4paper,12pt]{article}
    \\usepackage[utf8]{inputenc}
      \\usepackage[T1]{fontenc}
    \\usepackage{geometry}
    \\geometry{left=2.5cm,right=2cm,top=2cm,bottom=2cm}
    \\usepackage{array}
    \\usepackage{tabularx}
      \\usepackage{booktabs}

      \\begin{document}

      \\begin{center}
    {\\LARGE \\textbf{Arbeitszeitblatt}}\\\\[0.8cm]
      \\end{center}

    \\noindent\\textbf{Für Monat:} {{REPORT_MONTH}} \\\\[0.2cm]
    \\textbf{Name:} {{PROJECT_NAME}} \\\\[0.2cm]
    \\textbf{Einrichtung:} Projekt {{PROJECT_ID}}

    \\renewcommand{\\arraystretch}{1.3}

    \\noindent
    \\begin{tabularx}{\\textwidth}{| l | p{2cm} | p{2cm} | >{\\footnotesize\\raggedright\\arraybackslash}X | p{3cm} |}
    \\hline
    \\textbf{Tagesdatum} & \\textbf{Uhrzeit Beginn} & \\textbf{Uhrzeit Ende} & \\textbf{Bemerkung} & \\textbf{Arbeitszeit ohne Pause} \\\\
    \\hline\\hline
    {{TIMESLOT_ROWS}}
    \\hline\\hline
    \\multicolumn{4}{|r|}{\\textbf{IST-Arbeitszeit des Abrechnungsmonats:}} & {{IST_ARBEITSZEIT}} \\\\
    \\hline
    \\multicolumn{4}{|r|}{\\textbf{Zeitübertrag aus dem Vormonat:}} & {{ZEITUEBERTRAG_VORMONAT}} \\\\
    \\hline\\hline
    \\multicolumn{4}{|r|}{\\textbf{Summe (IST+Übertrag):}} & {{SUMME_IST_UEBERTRAG}} \\\\
    \\hline
    \\multicolumn{4}{|r|}{\\textbf{SOLL-Arbeitszeit des Abrechnungsmonats:}} & {{SOLL_ARBEITSZEIT}} \\\\
    \\hline\\hline
    \\multicolumn{4}{|r|}{\\textbf{Zeitübertrag auf nächsten Monat:}} & {{ZEITUEBERTRAG_NAECHSTER_MONAT}} \\\\
    \\hline\\hline
    \\end{tabularx}

    \\vspace{1cm}
    \\noindent
    \\begin{tabularx}{\\textwidth}{@{}X@{}}
    \\textbf{Mitarbeiter(in)} \\\\[0.3cm]
    \\textbf{Datum:} {{GENERATED_DATE}} \\hfill \\textbf{Unterschrift:} \\rule{6cm}{0.4pt}\\\\[1.8cm]
    \\textbf{Vorgesetzte(r)}\\\\[0.3cm]
    \\textbf{Datum:} \\rule{4cm}{0.4pt} \\hfill \\textbf{Unterschrift:} \\rule{6cm}{0.4pt}\\
    \\end{tabularx}

      \\end{document}
      """;

  private final ProjectRepository projectRepository;
  private final TimeslotRepository timeslotRepository;
  private final LatexCompiler latexCompiler;

  public ProjectReportPdfService(
      ProjectRepository projectRepository,
      TimeslotRepository timeslotRepository,
      LatexCompiler latexCompiler) {
    this.projectRepository = projectRepository;
    this.timeslotRepository = timeslotRepository;
    this.latexCompiler = latexCompiler;
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
    int transferFromPreviousMonthMinutes = 0;
    int sumIstAndTransferMinutes = totalMinutes + transferFromPreviousMonthMinutes;
    int sollArbeitszeitMinutes = totalMinutes;
    int transferToNextMonthMinutes = sumIstAndTransferMinutes - sollArbeitszeitMinutes;

    String template = readTemplate();
    String latex =
        template
            .replace("{{PROJECT_ID}}", escapeLatex(project.getSlug()))
            .replace("{{PROJECT_NAME}}", escapeLatex(project.getName()))
            .replace(
                "{{REPORT_MONTH}}",
                escapeLatex(capitalizeMonth(month.format(MONTH_LABEL_FORMATTER))))
            .replace("{{IST_ARBEITSZEIT}}", formatHoursAndMinutes(totalMinutes))
            .replace(
                "{{ZEITUEBERTRAG_VORMONAT}}",
                formatHoursAndMinutes(transferFromPreviousMonthMinutes))
            .replace("{{SUMME_IST_UEBERTRAG}}", formatHoursAndMinutes(sumIstAndTransferMinutes))
            .replace("{{SOLL_ARBEITSZEIT}}", formatHoursAndMinutes(sollArbeitszeitMinutes))
            .replace(
                "{{ZEITUEBERTRAG_NAECHSTER_MONAT}}",
                formatSignedHoursAndMinutes(transferToNextMonthMinutes))
            .replace(
                "{{GENERATED_DATE}}",
                escapeLatex(LocalDateTime.now().format(GENERATED_DATE_FORMATTER)))
            .replace("{{TIMESLOT_ROWS}}", buildTimeslotRows(monthSlots));

    return latexCompiler.compileToPdf(latex);
  }

  private String readTemplate() {
    return LATEX_TEMPLATE;
  }

  private String buildTimeslotRows(List<TimeslotEntity> monthSlots) {
    if (monthSlots.isEmpty()) {
      return "- & - & - & Keine Einträge in diesem Monat & 0h 00min \\\\\n\\hline";
    }

    return monthSlots.stream()
        .map(this::toTimeslotRow)
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
  }

  private String toTimeslotRow(TimeslotEntity slot) {
    String date = slot.getDate().format(DATE_FORMATTER);
    String start = slot.getStartTime().format(TIME_FORMATTER);
    LocalTime endTime = slot.getStartTime().plusMinutes(slot.getDurationMinutes());
    String end = endTime.format(TIME_FORMATTER);
    String remark = slot.getTitle();

    return String.format(
        Locale.ENGLISH,
        "%s & %s & %s & %s & %s \\\\\n\\hline",
        date,
        start,
        end,
        escapeLatex(remark),
        formatHoursAndMinutes(slot.getDurationMinutes()));
  }

  private String formatHoursAndMinutes(int totalMinutes) {
    int hours = Math.floorDiv(totalMinutes, 60);
    int minutes = Math.floorMod(totalMinutes, 60);
    return String.format(Locale.ENGLISH, "%dh %02dmin", hours, minutes);
  }

  private String formatSignedHoursAndMinutes(int totalMinutes) {
    String sign = totalMinutes < 0 ? "-" : "";
    return sign + formatHoursAndMinutes(Math.abs(totalMinutes));
  }

  private String capitalizeMonth(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return value.substring(0, 1).toUpperCase(Locale.GERMAN) + value.substring(1);
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
