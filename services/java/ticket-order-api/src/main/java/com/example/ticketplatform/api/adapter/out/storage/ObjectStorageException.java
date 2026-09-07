package com.example.ticketplatform.api.adapter.out.storage;

/**
 * Signals an unexpected failure in the object storage adapter itself (not a caller error), so
 * it is deliberately left unmapped by {@code EventControllerExceptionHandler} and falls through
 * to a default {@code 500} response instead of being confused with a client-facing conflict.
 */
class ObjectStorageException extends RuntimeException {

  ObjectStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
