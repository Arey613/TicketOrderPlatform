package com.example.ticketplatform.api.adapter.in.web;

import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EventController.class)
@Slf4j
class EventControllerExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<Void> notFound() {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(SecurityException.class)
  ResponseEntity<Void> forbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Void> badRequest(IllegalArgumentException exception) {
    log.warn("event.request.invalid", exception);
    return ResponseEntity.badRequest().build();
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Void> conflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }
}
