package com.tutortimetracker.api.service;

/** Compiles LaTeX source into PDF bytes. */
public interface LatexCompiler {

  /**
   * @param latexSource full LaTeX document source
   * @return generated PDF bytes
   */
  byte[] compileToPdf(String latexSource);
}
