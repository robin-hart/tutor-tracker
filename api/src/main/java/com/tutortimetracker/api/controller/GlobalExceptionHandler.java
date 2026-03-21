package com.tutortimetracker.api.controller;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.service.ProjectNotFoundException;
import com.tutortimetracker.api.service.StudentNotFoundException;
import com.tutortimetracker.api.service.TimeslotNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps backend exceptions to stable JSON responses for frontend consumption. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles unknown project references.
   *
   * @param exception thrown exception
   * @param request incoming HTTP request
   * @return standardized 404 payload
   */
  @ExceptionHandler(ProjectNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleProjectNotFound(
      ProjectNotFoundException exception, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Handles unknown student references.
   *
   * @param exception thrown exception
   * @param request incoming HTTP request
   * @return standardized 404 payload
   */
  @ExceptionHandler(StudentNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleStudentNotFound(
      StudentNotFoundException exception, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Handles unknown timeslot references.
   *
   * @param exception thrown exception
   * @param request incoming HTTP request
   * @return standardized 404 payload
   */
  @ExceptionHandler(TimeslotNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleTimeslotNotFound(
      TimeslotNotFoundException exception, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Handles bean validation failures.
   *
   * @param exception thrown exception
   * @param request incoming HTTP request
   * @return standardized 400 payload
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Validation failed");

    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI());
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Handles domain and parsing errors as bad requests.
   *
   * @param exception thrown exception
   * @param request incoming HTTP request
   * @return standardized 400 payload
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());
    return ResponseEntity.badRequest().body(body);
  }
}
