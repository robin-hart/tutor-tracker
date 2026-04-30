package com.tutortimetracker.api.service;

/** Compiles LaTeX source into PDF bytes. */
public interface LatexCompiler {

  /**
   * Compiles a full LaTeX document into PDF bytes.
   *
   * @param latexSource full LaTeX document source
   * @return generated PDF bytes
   */
  byte[] compileToPdf(String latexSource);
}
