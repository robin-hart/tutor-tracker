package com.tutortimetracker.api.service;

/** Thrown when a student key cannot be resolved. */
public class StudentNotFoundException extends RuntimeException {

  /**
   * Creates an exception for a missing student reference.
   *
   * @param message error details
   */
  public StudentNotFoundException(String message) {
    super(message);
  }
}
