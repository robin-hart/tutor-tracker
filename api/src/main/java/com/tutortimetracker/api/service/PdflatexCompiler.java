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

  private final String localLatexCommand;
  private final long timeoutSeconds;

  public PdflatexCompiler(
      @Value("${report.export.latex.local.command:${LATEX_COMMAND:pdflatex}}")
          String localLatexCommand,
      @Value("${report.export.latex.timeout-seconds:30}") long timeoutSeconds) {
    this.localLatexCommand = localLatexCommand;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public byte[] compileToPdf(String latexSource, List<LatexAsset> assets) {
    Path workDir = null;
    try {
      workDir = Files.createTempDirectory("monthly-report-");
      Path texFile = workDir.resolve("report.tex");
      Files.writeString(texFile, latexSource, StandardCharsets.UTF_8);

      writeAssets(workDir, assets);

      runLatex(workDir, texFile);

      Path pdfFile = workDir.resolve("report.pdf");
      if (!Files.exists(pdfFile)) {
        throw new PdfReportGenerationException(
            "LaTeX finished without producing a PDF file. Check your template and compiler logs.");
      }

      return Files.readAllBytes(pdfFile);
    } catch (IOException ex) {
      throw new PdfReportGenerationException(
          "Failed to generate report PDF. Ensure a local LaTeX compiler is installed and"
              + " accessible via LATEX_COMMAND.",
          ex);
    } finally {
      deleteDirectoryQuietly(workDir);
    }
  }

  private void writeAssets(Path workDir, List<LatexAsset> assets) throws IOException {
    if (assets == null || assets.isEmpty()) {
      return;
    }

    for (LatexAsset asset : assets) {
      if (asset == null || asset.fileName() == null || asset.fileName().isBlank()) {
        throw new IllegalArgumentException("LaTeX asset file name must be provided.");
      }
      if (asset.content() == null || asset.content().length == 0) {
        throw new IllegalArgumentException("LaTeX asset content must be provided.");
      }
      if (asset.fileName().contains("/") || asset.fileName().contains("\\")) {
        throw new IllegalArgumentException(
            "LaTeX asset file name must not contain path separators.");
      }

      Path target = workDir.resolve(asset.fileName()).normalize();
      if (!target.startsWith(workDir)) {
        throw new IllegalArgumentException(
            "LaTeX asset file name is not allowed: " + asset.fileName());
      }

      Files.write(target, asset.content());
    }
  }

  private void runLatex(Path workDir, Path texFile) throws IOException {
    List<String> command =
        List.of(
            localLatexCommand,
            "-interaction=nonstopmode",
            "-halt-on-error",
            "-output-directory",
            workDir.toString(),
            texFile.getFileName().toString());

    runProcess(command, workDir);
  }

  private void runProcess(List<String> command, Path workDir) throws IOException {

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
      LOGGER.error(
          "LaTeX compilation failed. Error snippet:\n{}\n--- Full output tail ---\n{}",
          extractErrorSnippet(output),
          abbreviate(output));
      throw new PdfReportGenerationException(
          "LaTeX compilation failed. Please check template data or server logs.");
    }
  }

  private String abbreviate(String text) {
    int max = 6000;
    if (text == null || text.length() <= max) {
      return text;
    }
    return "..." + text.substring(text.length() - max);
  }

  private String extractErrorSnippet(String output) {
    if (output == null || output.isBlank()) {
      return "No compiler output available.";
    }

    int marker = output.indexOf("! ");
    if (marker < 0) {
      marker = output.toLowerCase().indexOf("error");
    }

    if (marker < 0) {
      return abbreviate(output);
    }

    int from = Math.max(0, marker - 250);
    int to = Math.min(output.length(), marker + 1200);
    return output.substring(from, to);
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
