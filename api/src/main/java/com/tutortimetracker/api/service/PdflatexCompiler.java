package com.tutortimetracker.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Runs an external LaTeX compiler command and returns PDF bytes. */
@Component
public class PdflatexCompiler implements LatexCompiler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdflatexCompiler.class);

  private final String latexCommand;
  private final long timeoutSeconds;

  public PdflatexCompiler(
      @Value("${report.export.latex.command:pdflatex}") String latexCommand,
      @Value("${report.export.latex.timeout-seconds:30}") long timeoutSeconds) {
    this.latexCommand = latexCommand;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public byte[] compileToPdf(String latexSource) {
    Path workDir = null;
    try {
      workDir = Files.createTempDirectory("monthly-report-");
      Path texFile = workDir.resolve("report.tex");
      Files.writeString(texFile, latexSource, StandardCharsets.UTF_8);

      runLatex(workDir, texFile);

      Path pdfFile = workDir.resolve("report.pdf");
      if (!Files.exists(pdfFile)) {
        throw new PdfReportGenerationException(
            "LaTeX finished without producing a PDF file. Check your template and compiler logs.");
      }

      return Files.readAllBytes(pdfFile);
    } catch (IOException ex) {
      throw new PdfReportGenerationException(
          "Failed to generate report PDF. Ensure a LaTeX compiler is installed and available in"
              + " PATH.",
          ex);
    } finally {
      deleteDirectoryQuietly(workDir);
    }
  }

  private void runLatex(Path workDir, Path texFile) throws IOException {
    List<String> command =
        List.of(
            latexCommand,
            "-interaction=nonstopmode",
            "-halt-on-error",
            "-output-directory",
            workDir.toString(),
            texFile.getFileName().toString());

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(workDir.toFile());
    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    boolean completed;
    try {
      completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PdfReportGenerationException("LaTeX compilation was interrupted.", ex);
    }

    if (!completed) {
      process.destroyForcibly();
      throw new PdfReportGenerationException(
          "LaTeX compilation timed out after " + Duration.ofSeconds(timeoutSeconds) + ".");
    }

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.exitValue() != 0) {
      LOGGER.error("LaTeX compilation failed. Compiler output:\n{}", abbreviate(output));
      throw new PdfReportGenerationException(
          "LaTeX compilation failed. Please check template data or server logs.");
    }
  }

  private String abbreviate(String text) {
    int max = 500;
    if (text == null || text.length() <= max) {
      return text;
    }
    return text.substring(0, max) + "...";
  }

  private void deleteDirectoryQuietly(Path directory) {
    if (directory == null || !Files.exists(directory)) {
      return;
    }

    try (var paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).forEach(this::deletePathQuietly);
    } catch (IOException ignored) {
      // Best-effort cleanup only.
    }
  }

  private void deletePathQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Best-effort cleanup only.
    }
  }
}
