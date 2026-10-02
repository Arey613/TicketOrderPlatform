package com.example.ticketplatform.api.application.port.out;

public class EventDeletionConflictException extends RuntimeException {

  public EventDeletionConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
