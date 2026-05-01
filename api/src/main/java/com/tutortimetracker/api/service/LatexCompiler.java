package com.tutortimetracker.api.service;

/** Compiles LaTeX source into PDF bytes. */
public interface LatexCompiler {

  /**
   * @param latexSource full LaTeX document source
   * @param assets additional files needed during compilation
   * @return generated PDF bytes
   */
  byte[] compileToPdf(String latexSource, java.util.List<LatexAsset> assets);

  /**
   * @param latexSource full LaTeX document source
   * @return generated PDF bytes
   */
  default byte[] compileToPdf(String latexSource) {
    return compileToPdf(latexSource, java.util.List.of());
  }
}
