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

  private final String runner;
  private final String localLatexCommand;
  private final String dockerCommand;
  private final String dockerImage;
  private final String dockerExecutable;
  private final String composeService;
  private final String composeHostWorkRoot;
  private final String composeContainerWorkRoot;
  private final String composeProjectDirectory;
  private final long timeoutSeconds;

  public PdflatexCompiler(
      @Value("${report.export.latex.runner:compose}") String runner,
      @Value("${report.export.latex.local.command:${LATEX_COMMAND:pdflatex}}")
          String localLatexCommand,
      @Value("${report.export.latex.docker.command:pdflatex}") String dockerCommand,
      @Value("${report.export.latex.docker.image:tutor-tracker-latex:latest}") String dockerImage,
      @Value("${report.export.latex.docker.executable:docker}") String dockerExecutable,
      @Value("${report.export.latex.compose.service:latex}") String composeService,
      @Value("${report.export.latex.compose.host-work-root:.latex-work}")
          String composeHostWorkRoot,
      @Value("${report.export.latex.compose.container-work-root:/latex-work}")
          String composeContainerWorkRoot,
      @Value("${report.export.latex.compose.project-directory:.}") String composeProjectDirectory,
      @Value("${report.export.latex.timeout-seconds:30}") long timeoutSeconds) {
    this.runner = runner;
    this.localLatexCommand = localLatexCommand;
    this.dockerCommand = dockerCommand;
    this.dockerImage = dockerImage;
    this.dockerExecutable = dockerExecutable;
    this.composeService = composeService;
    this.composeHostWorkRoot = composeHostWorkRoot;
    this.composeContainerWorkRoot = composeContainerWorkRoot;
    this.composeProjectDirectory = composeProjectDirectory;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public byte[] compileToPdf(String latexSource) {
    Path workDir = null;
    try {
      workDir = createWorkDirectory();
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
          "Failed to generate report PDF. Ensure Docker is installed and running, or configure"
              + " runner=local with a local LaTeX compiler.",
          ex);
    } finally {
      deleteDirectoryQuietly(workDir);
    }
  }

  private void runLatex(Path workDir, Path texFile) throws IOException {
    List<String> command;
    Path processDirectory = workDir;

    if ("compose".equalsIgnoreCase(runner)) {
      String jobFolder = workDir.getFileName().toString();
      String containerOutputDirectory = composeContainerWorkRoot + "/" + jobFolder;
      command =
          List.of(
              dockerExecutable,
              "compose",
              "exec",
              "-T",
              "-w",
              containerOutputDirectory,
              composeService,
              dockerCommand,
              "-interaction=nonstopmode",
              "-halt-on-error",
              "-output-directory",
              containerOutputDirectory,
              texFile.getFileName().toString());
      processDirectory = Path.of(composeProjectDirectory);
    } else if ("docker".equalsIgnoreCase(runner)) {
      command =
          List.of(
              dockerExecutable,
              "run",
              "--rm",
              "-v",
              workDir.toAbsolutePath() + ":/work",
              "-w",
              "/work",
              dockerImage,
              dockerCommand,
              "-interaction=nonstopmode",
              "-halt-on-error",
              "-output-directory",
              "/work",
              texFile.getFileName().toString());
    } else {
      command =
          List.of(
              localLatexCommand,
              "-interaction=nonstopmode",
              "-halt-on-error",
              "-output-directory",
              workDir.toString(),
              texFile.getFileName().toString());
    }

    runProcess(command, processDirectory);
  }

  private Path createWorkDirectory() throws IOException {
    if (!"compose".equalsIgnoreCase(runner)) {
      return Files.createTempDirectory("monthly-report-");
    }

    Path hostRoot = Path.of(composeHostWorkRoot).toAbsolutePath();
    Files.createDirectories(hostRoot);
    return Files.createTempDirectory(hostRoot, "monthly-report-");
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
          "LaTeX compilation failed (runner='{}'). Error snippet:\n{}\n--- Full output tail"
              + " ---\n{}",
          runner,
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
