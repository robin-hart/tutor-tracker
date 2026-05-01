package com.tutortimetracker.api.service;

/** Represents an additional file needed for LaTeX compilation. */
public record LatexAsset(String fileName, byte[] content) {}
