package com.tutortimetracker.api.service;

import com.tutortimetracker.api.entity.ProjectEntity;
import com.tutortimetracker.api.entity.TimeslotEntity;
import com.tutortimetracker.api.repository.ProjectRepository;
import com.tutortimetracker.api.repository.TimeslotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Builds a project month report document and compiles it to PDF with LaTeX. */
@Service
public class ProjectReportPdfService {

  private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
      DateTimeFormatter.ofPattern("MMMM uuuu", Locale.GERMAN);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.uuuu");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final int MAX_TUTOR_NAME_LENGTH = 120;
  private static final int MAX_SIGNATURE_BYTES = 250_000;
  private static final String SIGNATURE_DATA_URL_PREFIX = "data:image/png;base64,";
  private static final String SIGNATURE_FILE_NAME = "tutor-signature.png";
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final String LATEX_TEMPLATE =
      """
    \\documentclass[a4paper,12pt]{article}
    \\usepackage[utf8]{inputenc}
      \\usepackage[T1]{fontenc}
    \\usepackage[german]{babel}
    \\usepackage{geometry}
    \\usepackage{graphicx}
    \\geometry{left=2.5cm,right=2cm,top=2cm,bottom=2cm}
    \\usepackage{array}
    \\usepackage{tabularx}
      \\usepackage{booktabs}

      \\begin{document}

      \\begin{center}
    {\\LARGE \\textbf{Arbeitszeitblatt}}\\\\[0.8cm]
      \\end{center}

    \\noindent\\textbf{Für Monat:} {{REPORT_MONTH}} \\\\[0.2cm]
      \\textbf{Name:} {{TUTOR_NAME}} \\\\[0.2cm]
  \\textbf{Einrichtung:} {{PROJECT_INSTITUTION}}

    \\renewcommand{\\arraystretch}{1.3}

    \\noindent
     \\begin{tabularx}{\\textwidth}{
     | l | p{2cm} | p{2cm} |
     >{\\footnotesize\\raggedright\\arraybackslash}X | p{3cm} |
     }
    \\hline
     \\textbf{Tagesdatum} & \\textbf{Uhrzeit Beginn} & \\textbf{Uhrzeit Ende}
       & \\textbf{Bemerkung} & \\textbf{Arbeitszeit ohne Pause} \\\\
    \\hline\\hline
    {{TIMESLOT_ROWS}}
    \\hline\\hline
     \\multicolumn{4}{|r|}{\\textbf{IST-Arbeitszeit des Abrechnungsmonats:}}
       & {{IST_ARBEITSZEIT}} \\\\
    \\hline
     \\multicolumn{4}{|r|}{\\textbf{Zeitübertrag aus dem Vormonat:}}
       & {{ZEITUEBERTRAG_VORMONAT}} \\\\
    \\hline\\hline
    \\multicolumn{4}{|r|}{\\textbf{Summe (IST+Übertrag):}} & {{SUMME_IST_UEBERTRAG}} \\\\
    \\hline
     \\multicolumn{4}{|r|}{\\textbf{SOLL-Arbeitszeit des Abrechnungsmonats:}}
       & {{SOLL_ARBEITSZEIT}} \\\\
    \\hline\\hline
     \\multicolumn{4}{|r|}{\\textbf{Zeitübertrag auf nächsten Monat:}}
       & {{ZEITUEBERTRAG_NAECHSTER_MONAT}} \\\\
    \\hline\\hline
    \\end{tabularx}

    \\vspace{1cm}
    \\noindent
    \\begin{tabularx}{\\textwidth}{@{}X@{}}
    \\textbf{Mitarbeiter(in)} \\\\[0.3cm]
     \\textbf{Datum:} \\today \\hfill
       \\textbf{Unterschrift:} {{TUTOR_SIGNATURE}}\\\\[1.8cm]
    \\textbf{Vorgesetzte(r)}\\\\[0.3cm]
      \\textbf{Datum:} \\rule{4cm}{0.4pt} \\hfill \\textbf{Unterschrift:}
      \\makebox[6cm][l]{\\rule{6cm}{0.4pt}}\\
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
    return exportProjectMonthPdf(projectId, monthKey, null, null);
  }

  /**
   * @param projectId project slug
   * @param monthKey month key in yyyy-MM format
   * @param tutorName optional tutor name to render
   * @param signatureDataUrl optional PNG data URL for the tutor signature
   * @return generated PDF bytes
   */
  public byte[] exportProjectMonthPdf(
      String projectId, String monthKey, String tutorName, String signatureDataUrl) {
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
    int targetMinutes = (int) Math.round(project.getTargetMonthHours() * 60.0);
    int transferFromPreviousMonthMinutes =
        calculateTransferFromPreviousMonthMinutes(project, month, targetMinutes);
    int sumIstAndTransferMinutes = totalMinutes + transferFromPreviousMonthMinutes;
    int sollArbeitszeitMinutes = targetMinutes;
    int transferToNextMonthMinutes = sumIstAndTransferMinutes - sollArbeitszeitMinutes;

    String normalizedTutorName = normalizeTutorName(tutorName);
    LatexAsset signatureAsset = decodeSignatureAsset(signatureDataUrl);
    String signatureLatex = buildSignatureLatex(signatureAsset != null);

    String template = readTemplate();
    String latex =
        template
            .replace("{{TUTOR_NAME}}", escapeLatex(normalizedTutorName))
            .replace("{{PROJECT_INSTITUTION}}", escapeLatex(project.getInstitution()))
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
            .replace("{{TIMESLOT_ROWS}}", buildTimeslotRows(monthSlots))
            .replace("{{TUTOR_SIGNATURE}}", signatureLatex);

    List<LatexAsset> assets = signatureAsset == null ? List.of() : List.of(signatureAsset);
    return latexCompiler.compileToPdf(latex, assets);
  }

  private String normalizeTutorName(String tutorName) {
    if (tutorName == null) {
      return "";
    }
    String normalized = tutorName.trim().replaceAll("\\s+", " ");
    if (normalized.isBlank()) {
      return "";
    }
    if (normalized.length() > MAX_TUTOR_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Tutor name must be " + MAX_TUTOR_NAME_LENGTH + " characters or fewer.");
    }
    return normalized;
  }

  private LatexAsset decodeSignatureAsset(String signatureDataUrl) {
    if (signatureDataUrl == null || signatureDataUrl.isBlank()) {
      return null;
    }
    if (!signatureDataUrl.startsWith(SIGNATURE_DATA_URL_PREFIX)) {
      throw new IllegalArgumentException("Signature must be a PNG data URL.");
    }

    String base64 = signatureDataUrl.substring(SIGNATURE_DATA_URL_PREFIX.length());
    byte[] decoded;
    try {
      decoded = java.util.Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Signature must be valid base64 PNG data.");
    }

    if (decoded.length > MAX_SIGNATURE_BYTES) {
      throw new IllegalArgumentException("Signature image is too large.");
    }

    if (!isPngSignature(decoded)) {
      throw new IllegalArgumentException("Signature must be a valid PNG image.");
    }

    return new LatexAsset(SIGNATURE_FILE_NAME, decoded);
  }

  private boolean isPngSignature(byte[] decoded) {
    if (decoded == null || decoded.length < PNG_SIGNATURE.length) {
      return false;
    }
    for (int index = 0; index < PNG_SIGNATURE.length; index++) {
      if (decoded[index] != PNG_SIGNATURE[index]) {
        return false;
      }
    }
    return true;
  }

  private String buildSignatureLatex(boolean hasSignature) {
    if (!hasSignature) {
      return "\\makebox[6cm][l]{\\rule{6cm}{0.4pt}}";
    }
    return "\\makebox[6cm][l]{\\raisebox{-0.15cm}{\\includegraphics[height=1.6cm]{"
        + SIGNATURE_FILE_NAME
        + "}}}";
  }

  private String readTemplate() {
    return LATEX_TEMPLATE;
  }

  private String buildTimeslotRows(List<TimeslotEntity> monthSlots) {
    if (monthSlots.isEmpty()) {
      return "";
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

    return String.format( // NOSONAR
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

  private int calculateTransferFromPreviousMonthMinutes(
      ProjectEntity project, YearMonth month, int targetMinutes) {
    List<TimeslotEntity> allProjectSlots = timeslotRepository.findByProject(project);

    LocalDate firstTimeslotDate =
        allProjectSlots.stream()
            .map(TimeslotEntity::getDate)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);

    if (firstTimeslotDate == null) {
      return 0;
    }

    YearMonth targetStartMonth = YearMonth.from(firstTimeslotDate);
    if (!month.isAfter(targetStartMonth)) {
      return 0;
    }

    LocalDate historyFrom = targetStartMonth.atDay(1);
    LocalDate historyTo = month.atDay(1);

    int historicalMinutes =
        allProjectSlots.stream()
            .filter(slot -> slot.getDate() != null)
            .filter(
                slot -> !slot.getDate().isBefore(historyFrom) && slot.getDate().isBefore(historyTo))
            .mapToInt(TimeslotEntity::getDurationMinutes)
            .sum();

    int monthsBeforeReport = (int) ChronoUnit.MONTHS.between(targetStartMonth, month);
    return historicalMinutes - (monthsBeforeReport * targetMinutes);
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
