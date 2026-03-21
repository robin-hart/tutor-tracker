package com.tutortimetracker.api.service;

/** Thrown when a student key cannot be resolved. */
public class StudentNotFoundException extends RuntimeException {

  /**
   * @param message error details
   */
  public StudentNotFoundException(String message) {
    super(message);
  }
}
