package com.tutortimetracker.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.tutortimetracker.api.model.ApiErrorResponse;
import com.tutortimetracker.api.service.PdfReportGenerationException;
import com.tutortimetracker.api.service.ProjectNotFoundException;
import com.tutortimetracker.api.service.StudentNotFoundException;
import com.tutortimetracker.api.service.TimeslotNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private HttpServletRequest request;

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    when(request.getRequestURI()).thenReturn("/api/test/path");
  }

  @Test
  void handleProjectNotFound_shouldReturn404Payload() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleProjectNotFound(new ProjectNotFoundException("project missing"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().status());
    assertEquals("project missing", response.getBody().message());
    assertEquals("/api/test/path", response.getBody().path());
  }

  @Test
  void handleStudentNotFound_shouldReturn404Payload() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleStudentNotFound(new StudentNotFoundException("student missing"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("student missing", response.getBody().message());
  }

  @Test
  void handleTimeslotNotFound_shouldReturn404Payload() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleTimeslotNotFound(new TimeslotNotFoundException("timeslot missing"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("timeslot missing", response.getBody().message());
  }

  @Test
  void handleIllegalArgument_shouldReturn400Payload() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleIllegalArgument(new IllegalArgumentException("bad input"), request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().status());
    assertEquals("bad input", response.getBody().message());
  }

  @Test
  void handlePdfGenerationError_shouldReturn503Payload() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handlePdfGenerationError(new PdfReportGenerationException("latex failed"), request);

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(503, response.getBody().status());
    assertEquals("latex failed", response.getBody().message());
  }

  @Test
  void handleDateTimeParse_shouldReturn400Payload() {
    DateTimeParseException exception = new DateTimeParseException("invalid month", "2026-99", 0);

    ResponseEntity<ApiErrorResponse> response = handler.handleDateTimeParse(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().status());
    assertEquals("/api/test/path", response.getBody().path());
  }

  @Test
  void handleValidation_shouldReturnFirstFieldErrorMessage() {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "month", "must not be blank"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("month must not be blank", response.getBody().message());
  }
}
