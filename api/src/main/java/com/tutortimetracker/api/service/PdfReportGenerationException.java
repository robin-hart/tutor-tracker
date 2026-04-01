package com.tutortimetracker.api.service;

/** Raised when a project report PDF cannot be generated. */
public class PdfReportGenerationException extends RuntimeException {

  public PdfReportGenerationException(String message) {
    super(message);
  }

  public PdfReportGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
