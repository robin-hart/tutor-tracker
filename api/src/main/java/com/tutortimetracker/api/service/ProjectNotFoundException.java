package com.tutortimetracker.api.service;

/** Thrown when a route references a project slug that does not exist. */
public class ProjectNotFoundException extends RuntimeException {

  /**
   * Creates an exception for a missing project reference.
   *
   * @param message error details
   */
  public ProjectNotFoundException(String message) {
    super(message);
  }
}
