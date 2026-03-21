package com.tutortimetracker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the TutorTimeTracker API service. */
@SpringBootApplication
public class TutorTimeTrackerApiApplication {

  /**
   * Starts the Spring Boot backend.
   *
   * @param args process arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(TutorTimeTrackerApiApplication.class, args);
  }
}
